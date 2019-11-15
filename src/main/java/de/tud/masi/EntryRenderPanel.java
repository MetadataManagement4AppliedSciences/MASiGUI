/*
 * Copyright 2019 Technische Universität Dresden.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tud.masi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.ehcache.Cache;
import org.json.XML;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.Page;
import com.vaadin.server.Page.Styles;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import edu.kit.dama.authorization.entities.IAuthorizationContext;
import edu.kit.dama.authorization.exceptions.UnauthorizedAccessAttemptException;
import edu.kit.dama.mdm.core.IMetaDataManager;
import edu.kit.dama.mdm.core.MetaDataManagement;
import edu.kit.dama.mdm.dataorganization.impl.jpa.FileNode;
import edu.kit.dama.ui.commons.util.UIUtils7;

/**
 * Render panel for a single search result entry in the PaginationPanel. This panel contains
 * everything to visualize the digital object.
 *
 * @author Richard Grunzke
 */
public class EntryRenderPanel extends CustomComponent {
    private static final long serialVersionUID = 1L;

    private final PaginationPanel parent;
    private GridLayout infoLayout;
    private HorizontalLayout contentLayout = new HorizontalLayout();
    private VerticalLayout buttonLayout = new VerticalLayout();
    private VerticalLayout thumbLayout = new VerticalLayout();
    private Label titleLabel = new Label("", ContentMode.HTML);
    private Label subjectLabel = new Label("", ContentMode.HTML);
    private Label creatorLabel = new Label("", ContentMode.HTML);
    private Label dateLabel = new Label("", ContentMode.HTML);
    private Label descriptionLabel = new Label("", ContentMode.HTML);
    private Label typeLabel = new Label("", ContentMode.HTML);
    private Label publisherLabel = new Label("", ContentMode.HTML);
    private Label identifierLabel = new Label("", ContentMode.HTML);
    private Label rightsLabel = new Label("", ContentMode.HTML);
    private Label entityLabel = new Label("", ContentMode.HTML);
    private Label entityRoleLabel = new Label("", ContentMode.HTML);
    private IAuthorizationContext pContextLocal;
    private String objectIdLocal;
    private Image image;
    private HashMap<String, String> valuesMapLocal;
    private HashMap<String, String> valuesMapLocalMETS = new HashMap<String, String>();
    private Cache<String, HashMap> valueCache;

    /**
     * Default constructor.
     *
     * @param pParent The parent component.
     * @param pContext The authorization context obtained from the main app.
     * @throws UnauthorizedAccessAttemptException
     * @throws IOException
     */
    public EntryRenderPanel(
            PaginationPanel pParent, IAuthorizationContext pContext, String objectId)
            throws UnauthorizedAccessAttemptException, IOException {
        parent = pParent;
        objectIdLocal = objectId;
        valuesMapLocal = parent.getParentUI().objectValuesMap.get(objectIdLocal);
        pContextLocal = pContext;
        buildMainLayout(pContext);
        setCompositionRoot(contentLayout);
    }

    @Override
    protected final void setCompositionRoot(Component compositionRoot) {
        super.setCompositionRoot(
                compositionRoot); // To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Build the main layout of the representation of one digital object.
     *
     * @param pContext The authorization context used to decide whether special administrative
     *     features are available or not.
     * @throws UnauthorizedAccessAttemptException
     * @throws IOException
     */
    private void buildMainLayout(IAuthorizationContext pContext)
            throws UnauthorizedAccessAttemptException, IOException {

        createObjectEntry();

        if (parent.getParentUI().siteName.equals(parent.getParentUI().siteNameCVMA)) {
            defineInfoLayoutCVMA();
        } else {
            defineInfoLayoutStandard();
        }

        thumbLayout = new VerticalLayout();
        thumbLayout.setSizeFull();
        thumbLayout.setSpacing(false);
        thumbLayout.setMargin(false);
        thumbLayout.setStyleName("white");
        thumbLayout.setHeight("150px");
        thumbLayout.setWidth("161px");

        // initialize thumbnail field and button
        setThumbButton();

        // build content layout
        buttonLayout = new VerticalLayout();
        buttonLayout.setSizeFull();
        buttonLayout.setSpacing(false);
        buttonLayout.setMargin(false);
        buttonLayout.setStyleName("white");
        buttonLayout.setHeight("150px");
        buttonLayout.setWidth("161px");

        // set download button
        setDownloadButton();

        // set metadata viewer button
        setMetadataViewerButton();

        // download button
        setImageDownloadButton();

        // map button
        setMapButton();

        // build content layout
        contentLayout = new HorizontalLayout();
        contentLayout.setSizeFull();
        contentLayout.setMargin(false);
        contentLayout.setMargin(new MarginInfo(false, false, true, false));
        contentLayout.setStyleName("white");
        contentLayout.addComponent(thumbLayout);
        contentLayout.addComponent(infoLayout);
        contentLayout.addComponent(buttonLayout);
        contentLayout.setComponentAlignment(thumbLayout, Alignment.TOP_LEFT);
        contentLayout.setComponentAlignment(infoLayout, Alignment.TOP_CENTER);
        contentLayout.setComponentAlignment(buttonLayout, Alignment.TOP_RIGHT);
        contentLayout.setExpandRatio(infoLayout, 11f);
    }

    /**
     * set download button and URL
     *
     * @throws FileNotFoundException
     */
    private void setDownloadButton() throws FileNotFoundException {
        Button downloadButton;
        if (parent.getParentUI().language.equals("Deutsch")) {
            downloadButton = new Button("Paket Download");
        } else {
            downloadButton = new Button("Download Package");
        }
        downloadButton.setWidth("100%");
        downloadButton.addStyleName(ValoTheme.BUTTON_SMALL);
        downloadButton.addStyleName(ValoTheme.LABEL_SMALL);
        buttonLayout.addComponent(downloadButton);
        buttonLayout.setComponentAlignment(downloadButton, Alignment.TOP_CENTER);

        // get base id of the object in question
        String sourceDirPath = valuesMapLocal.get("localpath");

        // zip on the fly
        StreamResource myResource4 =
                new StreamResource(
                        new StreamSource() {
                            private static final long serialVersionUID = 1L;
                            final ByteArrayOutputStream out = new ByteArrayOutputStream();

                            @Override
                            public InputStream getStream() {
                                Path directory2 = Paths.get(sourceDirPath);
                                try (ZipOutputStream zipStream = new ZipOutputStream(out)) {
                                    Files.walk(directory2)
                                            .filter(path -> !Files.isDirectory(path))
                                            .forEach(
                                                    path -> {
                                                        try {
                                                            addToZipStream(
                                                                    path, zipStream, sourceDirPath);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    });
                                } catch (IOException e) {
                                    System.out.println("Error while zipping." + e);
                                }
                                return new ByteArrayInputStream(out.toByteArray());
                            }
                        },
                        valuesMapLocal.get("digitalObjectId") + ".zip");

        FileDownloader fileDownloader3 = new FileDownloader(myResource4);
        fileDownloader3.extend(downloadButton);

        downloadButton.addClickListener(
                new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    public void buttonClick(ClickEvent event) {
                        parent.getParentUI().log("Click on package download button");
                    }
                });
    }

    /**
     * Adds an extra file to the zip archive, copying in the created date and a comment. Adapted
     * from
     * https://www.thecoderscorner.com/team-blog/java-and-jvm/63-writing-a-zip-file-in-java-using-zipoutputstream/
     *
     * @author: dave (https://www.thecoderscorner.com), Richard Grunzke
     * @param file to be archived
     * @param zipStream archive to contain the file.
     */
    private void addToZipStream(Path file, ZipOutputStream zipStream, String dirName)
            throws Exception {
        String inputFileName = file.toFile().getPath();
        try (FileInputStream inputStream = new FileInputStream(inputFileName)) {

            // create a new ZipEntry, which is basically another file
            // within the archive. We omit the path from the filename
            Path directory = Paths.get(dirName);
            ZipEntry entry = new ZipEntry(directory.relativize(file).toString());

            zipStream.putNextEntry(entry);

            // Now we copy the existing file into the zip archive. To do
            // this we write into the zip stream, the call to putNextEntry
            // above prepared the stream, we now write the bytes for this
            // entry. For another source such as an in memory array, you'd
            // just change where you read the information from.
            byte[] readBuffer = new byte[2048];
            int amountRead;
            int written = 0;

            while ((amountRead = inputStream.read(readBuffer)) > 0) {
                zipStream.write(readBuffer, 0, amountRead);
                written += amountRead;
            }
        } catch (IOException e) {
            throw new Exception("Unable to process " + inputFileName, e);
        }
    }

    /** metadata viewer button */
    private void setMetadataViewerButton() {
        Button metadataViewerButton;
        if (parent.getParentUI().language.equals("Deutsch")) {
            metadataViewerButton = new Button("Metadata ansehen");
        } else {
            metadataViewerButton = new Button("View Metadata");
        }
        metadataViewerButton.setWidth("100%");
        metadataViewerButton.addStyleName(ValoTheme.BUTTON_SMALL);
        metadataViewerButton.addStyleName(ValoTheme.LABEL_SMALL);
        buttonLayout.addComponent(metadataViewerButton);
        buttonLayout.setComponentAlignment(metadataViewerButton, Alignment.TOP_CENTER);

        metadataViewerButton.addClickListener(
                new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    //			@SuppressWarnings("unchecked")
                    public void buttonClick(ClickEvent event) {
                        parent.getParentUI().log("Click on View Metadata Button");
                        StringBuilder niceJson = null;
                        String path = valuesMapLocal.get("metspath");
                        String xmlDocument;
                        String metsString = "";
                        try {
                            xmlDocument = new String(Files.readAllBytes(Paths.get(path)));
                            metsString = XML.toJSONObject(xmlDocument).toString();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        JsonParser parser = new JsonParser();
                        JsonObject resultJsonObject;
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();

                        // create json jsonObject of result string
                        resultJsonObject =
                                (JsonObject)
                                        new JsonParser()
                                                .parse(
                                                        gson.toJson(
                                                                parser.parse(metsString)
                                                                        .getAsJsonObject()));

                        TreeMap<String, String> treeMap = new TreeMap<String, String>();

                        if (resultJsonObject
                                .getAsJsonObject("mets")
                                .getAsJsonArray("dmdSec")
                                .get(1)
                                .getAsJsonObject()
                                .getAsJsonObject("mdWrap")
                                .getAsJsonObject("xmlData")
                                .has("cvma:cvma")) {

                            Set<Map.Entry<String, JsonElement>> mySet =
                                    resultJsonObject
                                            .getAsJsonObject("mets")
                                            .getAsJsonArray("dmdSec")
                                            .get(1)
                                            .getAsJsonObject()
                                            .getAsJsonObject("mdWrap")
                                            .getAsJsonObject("xmlData")
                                            .getAsJsonObject("cvma:cvma")
                                            .entrySet();

                            for (Map.Entry<String, JsonElement> singleItem : mySet) {
                                treeMap.put(singleItem.getKey(), singleItem.getValue().toString());
                            }

                            for (Entry<String, String> entry : treeMap.entrySet()) {
                                niceJson.append(entry.getKey()).append(" - ").append(entry.getValue().replaceAll("\"", "")).append("\n");
                            }
                            //niceJson = new StringBuilder(niceJson.substring(4, niceJson.length() - 1));
                        }

                        GridLayout grid = new GridLayout(3, 1);
                        grid.setWidth("100%");
                        grid.setSpacing(false);
                        grid.setMargin(true);
                        grid.setStyleName("white");
                        grid.setColumnExpandRatio(0, 1.0f);
                        grid.setColumnExpandRatio(1, 1.0f);
                        grid.setColumnExpandRatio(2, 9999.0f);

                        for (Entry<String, String> entry : treeMap.entrySet()) {
                            Label keyLabel = new Label(entry.getKey(), ContentMode.HTML);
                            Label spaceLabel = new Label("&emsp;", ContentMode.HTML);
                            String tmpString = entry.getValue();
                            tmpString = tmpString.replaceAll("\",\"", ", ");
                            tmpString = tmpString.replaceAll("[\\[\\]\"]", "");
                            Label valueLabel = new Label(tmpString, ContentMode.HTML);
                            valueLabel.setWidth("100%");
                            grid.addComponent(keyLabel, 0, grid.getRows() - 1);
                            grid.addComponent(spaceLabel, 1, grid.getRows() - 1);
                            grid.addComponent(valueLabel, 2, grid.getRows() - 1);
                            grid.setRows(grid.getRows() + 1);
                        }
                        grid.setRows(grid.getRows() - 1);
                        Window metadataWindow;
                        if (parent.getParentUI().language.equals("Deutsch")) {
                            metadataWindow = new Window("Community-spezifische Metadaten");
                        } else {
                            metadataWindow = new Window("Community-specific metadata");
                        }
                        metadataWindow.setWidth(700.0f, Unit.PIXELS);
                        metadataWindow.setHeight(600.0f, Unit.PIXELS);
                        metadataWindow.center();
                        metadataWindow.setStyleName("mdsubwindow");
                        Styles styles = Page.getCurrent().getStyles();
                        styles.add(
                                ".v-app .mdsubwindow { min-width: 600px !important; min-height: 300px !important; }");
                        metadataWindow.setContent(grid);
                        getUI().addWindow(metadataWindow);
                    }
                });
    }

    /** thumb button */
    private void setThumbButton() {
        String thumb;
        String small;
        if (valuesMapLocal.containsKey("localpath")
                && valuesMapLocal.containsKey("thumb")
                && valuesMapLocal.containsKey("small")) {
            Button thumbButton = new Button();
            if (valuesMapLocal.get("localpath") != null
                    && valuesMapLocal.get("thumb") != null
                    && valuesMapLocal.get("small") != null) {
                thumb = valuesMapLocal.get("localpath") + "data/" + valuesMapLocal.get("thumb");
                small = valuesMapLocal.get("localpath") + "data/" + valuesMapLocal.get("small");
                thumbButton.setHeight("150px");
                thumbButton.setWidth("150px");
                thumbButton.setResponsive(false);
                thumbButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
                thumbButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
                thumbButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
                FileResource thumbResource = new FileResource(new File(thumb));
                FileResource smallResource = new FileResource(new File(small));
                thumbButton.setIcon(thumbResource);
                image = new Image(null, smallResource);
                thumbButton.addClickListener(
                        new Button.ClickListener() {
                            private static final long serialVersionUID = 1L;

                            public void buttonClick(ClickEvent event) {
                                parent.getParentUI().log("Click on Thumb Button");
                                image = new Image(null, smallResource);
                                image.setResponsive(true);
                                final Window window = new Window("Small Image Version");
                                window.center();
                                window.setResizable(false);
                                window.setResponsive(true);
                                window.setContent(image);
                                // window.addCloseShortcut(KeyCode.ESCAPE);
                                getUI().addWindow(window);
                            }
                        });
                thumbLayout.addComponent(thumbButton);
                thumbLayout.setComponentAlignment(thumbButton, Alignment.TOP_CENTER);
            } else {
                thumbButton.setEnabled(false);
            }
        }
    }

    /** image download button */
    private void setImageDownloadButton() {
        String normal;
        if (valuesMapLocal.containsKey("localpath") && valuesMapLocal.containsKey("default")) {
            normal = valuesMapLocal.get("localpath") + "data/" + valuesMapLocal.get("default");
            if (valuesMapLocal.get("localpath") != null && valuesMapLocal.get("default") != null) {
                Button imageDownloadButton;
                if (parent.getParentUI().language.equals("Deutsch")) {
                    imageDownloadButton = new Button("Bild Download");
                } else {
                    imageDownloadButton = new Button("Download Image");
                }
                imageDownloadButton.setWidth("100%");
                buttonLayout.addComponent(imageDownloadButton);
                buttonLayout.setComponentAlignment(imageDownloadButton, Alignment.TOP_CENTER);
                imageDownloadButton.addStyleName(ValoTheme.BUTTON_SMALL);
                imageDownloadButton.addStyleName(ValoTheme.LABEL_SMALL);
                FileResource normalResource = new FileResource(new File(normal));
                FileDownloader fd = new FileDownloader(normalResource);
                fd.extend(imageDownloadButton);
                imageDownloadButton.addClickListener(
                        new Button.ClickListener() {
                            private static final long serialVersionUID = 1L;

                            public void buttonClick(ClickEvent event) {
                                parent.getParentUI().log("Click on Image Download Button");
                            }
                        });
            }
        }
    }

    /** map button */
    private void setMapButton() {
        if (valuesMapLocal.containsKey("osmlink")) {
            if (valuesMapLocal.get("osmlink") != null) {
                Button mapButton;
                if (parent.getParentUI().language.equals("Deutsch")) {
                    mapButton = new Button("Kartenansicht");
                } else {
                    mapButton = new Button("View on Map");
                }
                mapButton.setWidth("100%");
                buttonLayout.addComponent(mapButton);
                buttonLayout.setComponentAlignment(mapButton, Alignment.TOP_CENTER);
                mapButton.addStyleName(ValoTheme.BUTTON_SMALL);
                mapButton.addStyleName(ValoTheme.LABEL_SMALL);
                mapButton.addClickListener(
                        new Button.ClickListener() {
                            private static final long serialVersionUID = 1L;

                            public void buttonClick(ClickEvent event) {
                                parent.getParentUI().log("Click on Map Button");
                                Button LinkButton;
                                float width = 600;
                                float height = 600;
                                double boundingboxFactor = 0.99999;
                                String link =
                                        "<iframe width=\""
                                                + width * 0.96
                                                + "\" height=\""
                                                + height * 0.83
                                                + "\" frameborder=\"0\" "
                                                + "scrolling=\"no\" marginheight=\"0\" marginwidth=\"0\" "
                                                + "src=\"https://www.openstreetmap.org/export/embed.html?bbox="
                                                + Double.parseDouble(
                                                                valuesMapLocal.get(
                                                                        "cvma:GPSLongitude"))
                                                        * boundingboxFactor
                                                + "%2C"
                                                + Double.parseDouble(
                                                                valuesMapLocal.get(
                                                                        "cvma:GPSLatitude"))
                                                        * boundingboxFactor
                                                + "%2C"
                                                + Double.parseDouble(
                                                                valuesMapLocal.get(
                                                                        "cvma:GPSLongitude"))
                                                        / boundingboxFactor
                                                + "%2C"
                                                + Double.parseDouble(
                                                                valuesMapLocal.get(
                                                                        "cvma:GPSLatitude"))
                                                        / boundingboxFactor
                                                + "&amp;layer=mapnik&amp;marker="
                                                + valuesMapLocal.get("cvma:GPSLatitude")
                                                + "%2C"
                                                + valuesMapLocal.get("cvma:GPSLongitude")
                                                + "\" style=\"border: 1px solid black\"></iframe>";
                                Label browser = new Label(link, ContentMode.HTML);
                                final VerticalLayout content = new VerticalLayout();
                                content.setMargin(true);
                                content.setSizeFull();
                                final HorizontalLayout linkLayout = new HorizontalLayout();
                                if (parent.getParentUI().language.equals("Deutsch")) {
                                    LinkButton = new Button("In Tab ansehen");
                                } else {
                                    LinkButton = new Button("View in tab");
                                }
                                LinkButton.addStyleName(ValoTheme.BUTTON_SMALL);
                                LinkButton.addStyleName(ValoTheme.LABEL_SMALL);
                                BrowserWindowOpener opener =
                                        new BrowserWindowOpener(
                                                new ExternalResource(
                                                        valuesMapLocal.get("osmlink")));
                                opener.setFeatures("");
                                opener.extend(LinkButton);
                                linkLayout.addComponent(LinkButton);
                                content.addComponent(linkLayout);
                                content.addComponent(browser);
                                content.setExpandRatio(browser, 1.0f);
                                Window mapWindow;
                                if (parent.getParentUI().language.equals("Deutsch")) {
                                    mapWindow = new Window("Lokation");
                                } else {
                                    mapWindow = new Window("Location");
                                }
                                mapWindow.setWidth(width, Unit.PIXELS);
                                mapWindow.setHeight(height, Unit.PIXELS);
                                mapWindow.setResizable(false);
                                mapWindow.center();
                                mapWindow.setContent(content);
                                getUI().addWindow(mapWindow);
                            }
                        });
            }
        }
    }

    private void defineInfoLayoutStandard() {
        String[] keys = new String[10];

        // initialize Title label
        keys[0] = "dc:title";
        if (valuesMapLocal.containsKey(keys[0]) && valuesMapLocal.get(keys[0]) != null) {
            titleLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().titleString
                                    + ": </b>"
                                    + valuesMapLocal.get(keys[0]),
                            ContentMode.HTML);
        } else {
            titleLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().titleString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
        }
        titleLabel.setWidth("100%");

        // initialize Subject label
        subjectLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().subjectString
                                + ": </b>"
                                + valuesMapLocal.get("dc:subject"),
                        ContentMode.HTML);
        subjectLabel.setWidth("100%");

        // initialize creator label
        keys[0] = "dc:creator";
        if (valuesMapLocal.containsKey(keys[0]) && valuesMapLocal.get(keys[0]) != null) {
            creatorLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().creatorString
                                    + ": </b>"
                                    + valuesMapLocal.get(keys[0]),
                            ContentMode.HTML);
        } else {
            creatorLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().creatorString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
        }
        creatorLabel.setWidth("100%");

        // initialize upload date label
        dateLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().dateString
                                + ": </b>"
                                + valuesMapLocal.get("dc:date"),
                        ContentMode.HTML);
        dateLabel.setWidth("100%");

        // initialize Type label
        typeLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().typeString
                                + ": </b>"
                                + valuesMapLocal.get("cvma:Type"),
                        ContentMode.HTML);
        typeLabel.setWidth("100%");

        // initialize Publisher label
        publisherLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().publisherString
                                + ": </b>"
                                + valuesMapLocal.get("dc:publisher"),
                        ContentMode.HTML);
        publisherLabel.setWidth("100%");

        // initialize identifier label
        identifierLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().idString
                                + ": </b>"
                                + valuesMapLocal.get("dc:identifier"),
                        ContentMode.HTML);
        identifierLabel.setWidth("100%");

        // initialize Rights label
        if (valuesMapLocal.get("xmprights:UsageTerms").equals("CC BY-NC 4.0")) {
            rightsLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().rightsString
                                    + ": </b>"
                                    + "<a href=\"https://creativecommons.org/licenses/by-nc/4.0/\">"
                                    + valuesMapLocal.get("xmprights:UsageTerms")
                                    + "</a>",
                            ContentMode.HTML);
        } else {
            rightsLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().rightsString
                                    + ": </b>"
                                    + valuesMapLocal.get("xmprights:UsageTerms"),
                            ContentMode.HTML);
        }
        rightsLabel.setWidth("100%");

        // initialize Description label
        descriptionLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().descriptionString
                                + ": </b>"
                                + valuesMapLocal.get("dc:description"),
                        ContentMode.HTML);
        descriptionLabel.setWidth("100%");

        // arrange search result components
        infoLayout =
                new UIUtils7.GridLayoutBuilder(2, 5)
                        .addComponent(titleLabel, 0, 0, 1, 1)
                        .addComponent(subjectLabel, 1, 0, 1, 1)
                        .addComponent(creatorLabel, 0, 1, 1, 1)
                        .addComponent(dateLabel, 1, 1, 1, 1)
                        .addComponent(typeLabel, 0, 2, 1, 1)
                        .addComponent(publisherLabel, 1, 2, 1, 1)
                        .addComponent(identifierLabel, 0, 3, 1, 1)
                        .addComponent(rightsLabel, 1, 3, 1, 1)
                        .addComponent(descriptionLabel, 0, 4, 2, 1)
                        .getLayout();
        infoLayout.setSizeFull();
        infoLayout.setSpacing(false);
        infoLayout.setMargin(false);
        infoLayout.setStyleName("white");
    }

    private void defineInfoLayoutCVMA() {
        String[] keys = new String[10];

        // initialize Title label
        keys[0] = "dc:title";
        if (valuesMapLocal.containsKey(keys[0]) && valuesMapLocal.get(keys[0]) != null) {
            titleLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().titleString
                                    + ": </b>"
                                    + valuesMapLocal.get(keys[0]),
                            ContentMode.HTML);
        } else {
            titleLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().titleString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
        }
        titleLabel.setWidth("100%");
        titleLabel.addStyleName("dawn-pink");

        // initialize location label
        keys[0] = "osmlink";
        keys[1] = "Iptc4xmpExt:City";
        keys[2] = "Iptc4xmpExt:ProvinceState";
        keys[3] = "Iptc4xmpExt:Sublocation";
        Label locationLabel;
        if (valuesMapLocal.containsKey(keys[1])
                && valuesMapLocal.containsKey(keys[2])
                && valuesMapLocal.containsKey(keys[3])
                && valuesMapLocal.get(keys[1]) != null
                && valuesMapLocal.get(keys[2]) != null
                && valuesMapLocal.get(keys[3]) != null) {
            locationLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().locationString
                                    + ": </b>"
                                    + valuesMapLocal.get(keys[1])
                                    + " ("
                                    + valuesMapLocal.get(keys[2])
                                    + "), "
                                    + valuesMapLocal.get(keys[3]),
                            ContentMode.HTML);
        } else {
            locationLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().locationString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
        }
        locationLabel.setWidth("100%");
        locationLabel.setStyleName("white");

        // initialize creator label
        keys[0] = "dc:creator";
        if (valuesMapLocal.containsKey(keys[0]) && valuesMapLocal.get(keys[0]) != null) {
            creatorLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().creatorString
                                    + ": </b>"
                                    + valuesMapLocal.get(keys[0]),
                            ContentMode.HTML);
        } else {
            creatorLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().creatorString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
        }
        creatorLabel.setWidth("100%");
        creatorLabel.setStyleName("dawn-pink");

        // initialize upload date label
        if (valuesMapLocal.get("cvma:AgeDeterminationStart") == null
                || valuesMapLocal.get("cvma:AgeDeterminationStart") == null) {
            dateLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().dateString
                                    + ": </b>"
                                    + " "
                                    + parent.getParentUI().unknownString
                                    + " ",
                            ContentMode.HTML);
            dateLabel.setWidth("100%");
        } else {
            String tmp1 =
                    valuesMapLocal.get("cvma:AgeDeterminationStart").split("-")[2]
                            + "."
                            + valuesMapLocal.get("cvma:AgeDeterminationStart").split("-")[1]
                            + "."
                            + valuesMapLocal.get("cvma:AgeDeterminationStart").split("-")[0];
            String tmp2 =
                    valuesMapLocal.get("cvma:AgeDeterminationEnd").split("-")[2]
                            + "."
                            + valuesMapLocal.get("cvma:AgeDeterminationEnd").split("-")[1]
                            + "."
                            + valuesMapLocal.get("cvma:AgeDeterminationEnd").split("-")[0];
            dateLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().dateString
                                    + ": </b>"
                                    + tmp1
                                    + " - "
                                    + tmp2,
                            ContentMode.HTML);
            dateLabel.setWidth("100%");
        }
        dateLabel.addStyleName("white");

        // initialize Type label
        typeLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().typeString
                                + ": </b>"
                                + valuesMapLocal.get("cvma:Type"),
                        ContentMode.HTML);
        typeLabel.setWidth("100%");
        typeLabel.addStyleName("white");

        // initialize Publisher label
        publisherLabel =
                new Label(
                        "<b>"
                                + parent.getParentUI().publisherString
                                + ": </b>"
                                + valuesMapLocal.get("dc:publisher"),
                        ContentMode.HTML);
        publisherLabel.setWidth("100%");
        publisherLabel.addStyleName("dawn-pink");

        // initialize Rights label
        if (valuesMapLocal.containsKey("xmprights:UsageTerms")
                && valuesMapLocal.get("xmprights:UsageTerms") != null) {
            if (valuesMapLocal.get("xmprights:UsageTerms").equals("CC BY-NC 4.0")) {
                rightsLabel =
                        new Label(
                                "<b>"
                                        + parent.getParentUI().rightsString
                                        + ": </b>"
                                        + "<a href=\"https://creativecommons.org/licenses/by-nc/4.0/\">"
                                        + valuesMapLocal.get("xmprights:UsageTerms")
                                        + "</a>",
                                ContentMode.HTML);
            } else {
                rightsLabel =
                        new Label(
                                "<b>"
                                        + parent.getParentUI().rightsString
                                        + "s: </b>"
                                        + valuesMapLocal.get("xmprights:UsageTerms"),
                                ContentMode.HTML);
            }
        } else {
            rightsLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().rightsString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
        }
        rightsLabel.setWidth("100%");
        rightsLabel.addStyleName("dawn-pink");

        // initialize Entity label
        keys[0] = "cvma:EntityName";
        String tmpString;
        if (valuesMapLocal.containsKey(keys[0]) && valuesMapLocal.get(keys[0]) != null) {
            tmpString =
                    "<b>"
                            + parent.getParentUI().entityString
                            + ": </b>"
                            + valuesMapLocal.get(keys[0]);
            tmpString = tmpString.replaceAll("\",\"", ", ");
            tmpString = tmpString.replaceAll("[\\[\\]\"]", "");
            entityLabel = new Label(tmpString, ContentMode.HTML);
        } else {
            entityLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().entityString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
        }
        entityLabel.setWidth("100%");
        entityLabel.addStyleName("dawn-pink");

        // initialize EntityRole label
        keys[0] = "cvma:EntityName";
        keys[1] = "cvma:EntityRole";
        tmpString = "";
        entityRoleLabel = new Label(tmpString, ContentMode.HTML);
        entityRoleLabel.setWidth("100%");
        entityRoleLabel.addStyleName("dawn-pink");
        if (valuesMapLocal.containsKey(keys[0]) && valuesMapLocal.get(keys[0]) != null) {
            if (valuesMapLocal.containsKey(keys[1]) && valuesMapLocal.get(keys[1]) != null) {
                tmpString =
                        "<b>"
                                + parent.getParentUI().entityroleString
                                + ": </b>"
                                + valuesMapLocal.get(keys[1]);
                tmpString = tmpString.replaceAll("\",\"", ", ");
                tmpString = tmpString.replaceAll("[\\[\\]\"]", "");
                entityRoleLabel = new Label(tmpString, ContentMode.HTML);
            } else {
                tmpString =
                        "<b>"
                                + parent.getParentUI().entityroleString
                                + ": </b>"
                                + parent.getParentUI().unknownString;
                entityRoleLabel = new Label(tmpString, ContentMode.HTML);
            }
        } else {
            tmpString = "&emsp;";
            entityRoleLabel = new Label(tmpString, ContentMode.HTML);
        }
        entityRoleLabel.setWidth("100%");
        entityRoleLabel.addStyleName("dawn-pink");

        // initialize Description label
        if (valuesMapLocal.get("cvma:IconclassDescription") == null) {
            descriptionLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().descriptionString
                                    + ": </b>"
                                    + parent.getParentUI().unknownString,
                            ContentMode.HTML);
            descriptionLabel.setWidth("100%");
        } else {
            descriptionLabel =
                    new Label(
                            "<b>"
                                    + parent.getParentUI().descriptionString
                                    + ": </b>"
                                    + valuesMapLocal.get("cvma:IconclassDescription"),
                            ContentMode.HTML);
            descriptionLabel.setWidth("100%");
        }

        // arrange search result components
        infoLayout =
                new UIUtils7.GridLayoutBuilder(2, 6)
                        .addComponent(titleLabel, 0, 0, 1, 1)
                        .addComponent(creatorLabel, 1, 0, 1, 1)
                        .addComponent(typeLabel, 0, 1, 1, 1)
                        .addComponent(dateLabel, 1, 1, 1, 1)
                        .addComponent(publisherLabel, 0, 2, 1, 1)
                        .addComponent(rightsLabel, 1, 2, 1, 1)
                        .addComponent(locationLabel, 0, 3, 2, 1)
                        .addComponent(entityLabel, 0, 4, 1, 1)
                        .addComponent(entityRoleLabel, 1, 4, 1, 1)
                        .addComponent(descriptionLabel, 0, 5, 2, 1)
                        .getLayout();
        infoLayout.setSizeFull();
        infoLayout.setSpacing(false);
        infoLayout.setMargin(false);
        infoLayout.setStyleName("white");
    }

    @SuppressWarnings({"unused", "unchecked"})
    private void createObjectEntry() throws UnauthorizedAccessAttemptException, IOException {
        try {
            long startTime = System.nanoTime();
            try {
                valueCache = parent.valueCache;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!valueCache.containsKey(objectIdLocal)) {
                fillObjectValuesMap();
            } else {
                valuesMapLocal = valueCache.get(objectIdLocal);
            }
        } catch (UnauthorizedAccessAttemptException e2) {
            e2.printStackTrace();
        }
    }

    private void fillObjectValuesMap() throws UnauthorizedAccessAttemptException, IOException {
        String key;
        String value;

        // Map that the key value pairs extract form mets are stored in
        valuesMapLocalMETS = new HashMap<String, String>();

        IMetaDataManager mdmDo =
                MetaDataManagement.getMetaDataManagement().getMetaDataManager("DataOrganizationPU");
        mdmDo.setAuthorizationContext(pContextLocal);
        FileNode fileNode;
        fileNode =
                mdmDo.findSingleResult(
                        "Select t FROM DataOrganizationNode t WHERE t.name=?1",
                        new Object[] {"mets_" + objectIdLocal + ".xml"},
                        FileNode.class);
        mdmDo.close();
        String path = fileNode.getLogicalFileName().getStringRepresentation().substring(5);
        String xmlDocument = new String(Files.readAllBytes(Paths.get(path)));
        String metsString = XML.toJSONObject(xmlDocument).toString();

        // add path to mets file
        valuesMapLocalMETS.put("metspath", path);

        // create new json parser
        JsonParser jsonparser = new JsonParser();

        // create json object with metsString as input
        JsonObject jsonObject = jsonparser.parse(metsString).getAsJsonObject();

        // digital object ID
        key = "digitalObjectId";
        value = jsonObject.getAsJsonObject("mets").getAsJsonPrimitive("OBJID").getAsString();
        valuesMapLocalMETS.put(key, value);

        // createDate
        valuesMapLocalMETS.put(
                "createDate",
                jsonObject
                        .getAsJsonObject("mets")
                        .getAsJsonObject("metsHdr")
                        .getAsJsonPrimitive("CREATEDATE")
                        .getAsString());

        // loop for mets.amdSec.sourceMD array
        for (int i = 0;
                i
                        < jsonObject
                                .getAsJsonObject("mets")
                                .getAsJsonObject("amdSec")
                                .getAsJsonArray("sourceMD")
                                .size();
                i++) {

            // for KIT-DM-BASEMETADATA part
            if (jsonObject
                    .getAsJsonObject("mets")
                    .getAsJsonObject("amdSec")
                    .getAsJsonArray("sourceMD")
                    .get(i)
                    .getAsJsonObject()
                    .get("ID")
                    .getAsJsonPrimitive()
                    .getAsString()
                    .equals("KIT-DM-BASEMETADATA")) {

                // baseId
                valuesMapLocalMETS.put(
                        "baseId",
                        jsonObject
                                .getAsJsonObject("mets")
                                .getAsJsonObject("amdSec")
                                .getAsJsonArray("sourceMD")
                                .get(i)
                                .getAsJsonObject()
                                .getAsJsonObject("mdWrap")
                                .getAsJsonObject("xmlData")
                                .getAsJsonObject("basemetadata")
                                .getAsJsonObject("digitalObject")
                                .getAsJsonPrimitive("baseId")
                                .getAsString());

                // distinguishedName
                valuesMapLocalMETS.put(
                        "distinguishedName",
                        jsonObject
                                .getAsJsonObject("mets")
                                .getAsJsonObject("amdSec")
                                .getAsJsonArray("sourceMD")
                                .get(i)
                                .getAsJsonObject()
                                .getAsJsonObject("mdWrap")
                                .getAsJsonObject("xmlData")
                                .getAsJsonObject("basemetadata")
                                .getAsJsonObject("digitalObject")
                                .getAsJsonObject("uploader")
                                .getAsJsonPrimitive("distinguishedName")
                                .getAsString());
            }

            // for KIT-DM-DATAORGANIZATION part
            if (jsonObject
                    .getAsJsonObject("mets")
                    .getAsJsonObject("amdSec")
                    .getAsJsonArray("sourceMD")
                    .get(i)
                    .getAsJsonObject()
                    .get("ID")
                    .getAsJsonPrimitive()
                    .getAsString()
                    .equals("KIT-DM-DATAORGANIZATION")) {

                // loop for mets.amdSec.sourceMD.mdWrap.xmlData.dataOrganization.view array
                for (int j = 0;
                        j
                                < jsonObject
                                        .getAsJsonObject("mets")
                                        .getAsJsonObject("amdSec")
                                        .getAsJsonArray("sourceMD")
                                        .get(i)
                                        .getAsJsonObject()
                                        .getAsJsonObject("mdWrap")
                                        .getAsJsonObject("xmlData")
                                        .getAsJsonObject("dataOrganization")
                                        .getAsJsonArray("view")
                                        .size();
                        j++) {

                    if (jsonObject
                            .getAsJsonObject("mets")
                            .getAsJsonObject("amdSec")
                            .getAsJsonArray("sourceMD")
                            .get(i)
                            .getAsJsonObject()
                            .getAsJsonObject("mdWrap")
                            .getAsJsonObject("xmlData")
                            .getAsJsonObject("dataOrganization")
                            .getAsJsonArray("view")
                            .get(j)
                            .getAsJsonObject()
                            .getAsJsonObject("root")
                            .getAsJsonObject("children")
                            .get("child")
                            .isJsonArray()) {

                        // loop for
                        // mets.amdSec.sourceMD.mdWrap.xmlData.dataOrganization.view.root.children.child array
                        for (int k = 0;
                                k
                                        < jsonObject
                                                .getAsJsonObject("mets")
                                                .getAsJsonObject("amdSec")
                                                .getAsJsonArray("sourceMD")
                                                .get(i)
                                                .getAsJsonObject()
                                                .getAsJsonObject("mdWrap")
                                                .getAsJsonObject("xmlData")
                                                .getAsJsonObject("dataOrganization")
                                                .getAsJsonArray("view")
                                                .get(j)
                                                .getAsJsonObject()
                                                .getAsJsonObject("root")
                                                .getAsJsonObject("children")
                                                .getAsJsonArray("child")
                                                .size();
                                k++) {

                            key =
                                    jsonObject
                                            .getAsJsonObject("mets")
                                            .getAsJsonObject("amdSec")
                                            .getAsJsonArray("sourceMD")
                                            .get(i)
                                            .getAsJsonObject()
                                            .getAsJsonObject("mdWrap")
                                            .getAsJsonObject("xmlData")
                                            .getAsJsonObject("dataOrganization")
                                            .getAsJsonArray("view")
                                            .get(j)
                                            .getAsJsonObject()
                                            .getAsJsonPrimitive("NS1:name")
                                            .getAsString();

                            value =
                                    jsonObject
                                            .getAsJsonObject("mets")
                                            .getAsJsonObject("amdSec")
                                            .getAsJsonArray("sourceMD")
                                            .get(i)
                                            .getAsJsonObject()
                                            .getAsJsonObject("mdWrap")
                                            .getAsJsonObject("xmlData")
                                            .getAsJsonObject("dataOrganization")
                                            .getAsJsonArray("view")
                                            .get(j)
                                            .getAsJsonObject()
                                            .getAsJsonObject("root")
                                            .getAsJsonObject("children")
                                            .getAsJsonArray("child")
                                            .get(k)
                                            .getAsJsonObject()
                                            .getAsJsonPrimitive("name")
                                            .getAsString()
                                            .toLowerCase();

                            List<String> imageextensions = new LinkedList<>();
                            imageextensions.add("jpg");
                            imageextensions.add("jpeg");
                            imageextensions.add("tif");
                            imageextensions.add("tiff");
                            imageextensions.add("gif");
                            imageextensions.add("bmp");
                            imageextensions.add("png");

                            // button for image download currently allows one image, even if more
                            // exist
                            if (imageextensions.contains(FilenameUtils.getExtension(value))) {
                                valuesMapLocalMETS.put(key, value);
                            }
                        }
                    } else {
                        // file names of data files

                        if (!jsonObject
                                .getAsJsonObject("mets")
                                .getAsJsonObject("amdSec")
                                .getAsJsonArray("sourceMD")
                                .get(i)
                                .getAsJsonObject()
                                .getAsJsonObject("mdWrap")
                                .getAsJsonObject("xmlData")
                                .getAsJsonObject("dataOrganization")
                                .getAsJsonArray("view")
                                .get(j)
                                .getAsJsonObject()
                                .getAsJsonObject("root")
                                .getAsJsonObject("children")
                                .getAsJsonObject("child")
                                .getAsJsonPrimitive("name")
                                .getAsString()
                                .equals("metadata")) {

                            key =
                                    jsonObject
                                            .getAsJsonObject("mets")
                                            .getAsJsonObject("amdSec")
                                            .getAsJsonArray("sourceMD")
                                            .get(i)
                                            .getAsJsonObject()
                                            .getAsJsonObject("mdWrap")
                                            .getAsJsonObject("xmlData")
                                            .getAsJsonObject("dataOrganization")
                                            .getAsJsonArray("view")
                                            .get(j)
                                            .getAsJsonObject()
                                            .getAsJsonPrimitive("NS1:name")
                                            .getAsString();

                            value =
                                    jsonObject
                                            .getAsJsonObject("mets")
                                            .getAsJsonObject("amdSec")
                                            .getAsJsonArray("sourceMD")
                                            .get(i)
                                            .getAsJsonObject()
                                            .getAsJsonObject("mdWrap")
                                            .getAsJsonObject("xmlData")
                                            .getAsJsonObject("dataOrganization")
                                            .getAsJsonArray("view")
                                            .get(j)
                                            .getAsJsonObject()
                                            .getAsJsonObject("root")
                                            .getAsJsonObject("children")
                                            .getAsJsonObject("child")
                                            .getAsJsonPrimitive("name")
                                            .getAsString();

                            valuesMapLocalMETS.put(key, value);
                        }
                    }
                }
            }
        }

        HashMap<String, Integer> dmdSections = new HashMap<String, Integer>();
        // loop for mets.dmdSec array
        for (int i = 0;
                i < jsonObject.getAsJsonObject("mets").getAsJsonArray("dmdSec").size();
                i++) {
            dmdSections.put(
                    jsonObject
                            .getAsJsonObject("mets")
                            .getAsJsonArray("dmdSec")
                            .get(i)
                            .getAsJsonObject()
                            .get("ID")
                            .getAsJsonPrimitive()
                            .getAsString(),
                    i);
        }
        // cvma values
        if (dmdSections.containsKey("CVMA")) {
            value = null;
            String[] keys = {
                "Iptc4xmpExt:Sublocation",
                "xmprights:UsageTerms",
                "cvma:Type",
                // 	  	            		 "xmlns:Iptc4xmpExt",
                "dc:creator",
                //   	            		 "xmlns:ns3",
                //   	            		 "xmlns:photoshop",
                "cvma:AgeDeterminationStart",
                //   	            		 "photoshop:Credit",
                "cvma:GPSLatitude",
                "dc:title",
                //   	            		 "xmprights:Owner",
                //   	            		 "cvma:PublishingStatus",
                "cvma:GPSLongitude",
                //   	            		 "Iptc4xmpExt:WorldRegion",
                //   	            		 "cvma:IconclassNotation",
                "Iptc4xmpExt:City",
                "cvma:AgeDeterminationEnd",
                //   	            		 "Iptc4xmpExt:CountryName",
                //   	            		 "xmlns:xmprights",
                //   	            		 "dc:relation",
                //   	            		 "xmprights:WebStatement",
                //   	            		 "cvma:Volume",
                "cvma:IconclassDescription",
                //   	            		 "xmprights:Marked",
                //   	            		 "dc:identifier",
                //   	            		 "xmlns:cvma",
                //   	            		 "xmlns:dc",
                "cvma:EntityName",
                "cvma:EntityRole",
                "Iptc4xmpExt:ProvinceState",
                "dc:publisher"
            };
            for (String keyEntry : keys) {
                value = extractDmdSecMD("cvma:cvma", keyEntry, jsonObject, dmdSections.get("CVMA"));
                if (value != null) {
                    valuesMapLocalMETS.put(keyEntry, value);
                }
            }

        } else if (dmdSections.containsKey("DUBLIN-CORE")) {
            // dublin core values
            value = null;
            String[] keys = {
                "dc:description",
                "dc:subject",
                "dc:creator",
                "dc:rights",
                "dc:format",
                "dc:type",
                "dc:title",
                "dc:date",
                "dc:identifier",
                "dc:publisher"
            };
            for (String keyEntry : keys) {
                value =
                        extractDmdSecMD(
                                "oai_dc:dc", keyEntry, jsonObject, dmdSections.get("DUBLIN-CORE"));
                valuesMapLocalMETS.put(keyEntry, value);
            }
        }

        String date = valuesMapLocalMETS.get("createDate");
        String[] tmp2 = date.split("T");
        String[] tmp3 = tmp2[0].split("-");
        if (tmp3[1].substring(0, 0).equals("0")) {
            tmp3[1] = tmp3[1].substring(1, 1);
        }
        int tmp4 = Integer.parseInt(tmp3[1]) - 1;
        tmp3[1] = String.valueOf(tmp4);
        String localPath =
                "/data2/"
                        + tmp3[0]
                        + "/"
                        + tmp3[1].replaceFirst("^0+(?!$)", "")
                        + "/"
                        + tmp3[2].replaceFirst("^0+(?!$)", "")
                        + "/"
                        + valuesMapLocalMETS.get("distinguishedName")
                        + "/ingest_"
                        + valuesMapLocalMETS.get("baseId")
                        + "_"
                        + valuesMapLocalMETS.get("digitalObjectId")
                        + "/";
        // store path in map of digital object
        valuesMapLocalMETS.put("localpath", localPath);

        // osm link
        if (valuesMapLocalMETS.containsKey("cvma:GPSLatitude")
                && valuesMapLocalMETS.containsKey("cvma:GPSLongitude")) {
            if (valuesMapLocalMETS.get("cvma:GPSLatitude") != null
                    && valuesMapLocalMETS.get("cvma:GPSLongitude") != null) {
                valuesMapLocalMETS.put(
                        "osmlink",
                        "https://www.openstreetmap.org/?mlat="
                                + valuesMapLocalMETS.get("cvma:GPSLatitude")
                                + "&mlon="
                                + valuesMapLocalMETS.get("cvma:GPSLongitude")
                                + "#map=18/"
                                + valuesMapLocalMETS.get("cvma:GPSLatitude")
                                + "/"
                                + valuesMapLocalMETS.get("cvma:GPSLongitude"));
            }
        }

        // put map of newly extracted key value pairs (valuesMapLocalMETS) into map as value with
        // digitalObjectId (id) as key
        parent.getParentUI()
                .objectValuesMap
                .put(valuesMapLocalMETS.get("digitalObjectId"), valuesMapLocalMETS);

        valueCache.put(valuesMapLocalMETS.get("digitalObjectId"), valuesMapLocalMETS);
        valuesMapLocal = valuesMapLocalMETS;
    }

    private String extractDmdSecMD(String section, String key, JsonObject jsonObject, int counter) {
        String value = null;
        if (jsonObject
                .getAsJsonObject("mets")
                .getAsJsonArray("dmdSec")
                .get(counter)
                .getAsJsonObject()
                .getAsJsonObject("mdWrap")
                .getAsJsonObject("xmlData")
                .has(section)) {
            if (jsonObject
                    .getAsJsonObject("mets")
                    .getAsJsonArray("dmdSec")
                    .get(counter)
                    .getAsJsonObject()
                    .getAsJsonObject("mdWrap")
                    .getAsJsonObject("xmlData")
                    .getAsJsonObject(section)
                    .has(key)) {
                if (jsonObject
                        .getAsJsonObject("mets")
                        .getAsJsonArray("dmdSec")
                        .get(counter)
                        .getAsJsonObject()
                        .getAsJsonObject("mdWrap")
                        .getAsJsonObject("xmlData")
                        .getAsJsonObject(section)
                        .get(key)
                        .isJsonArray()) {
                    value =
                            jsonObject
                                    .getAsJsonObject("mets")
                                    .getAsJsonArray("dmdSec")
                                    .get(counter)
                                    .getAsJsonObject()
                                    .getAsJsonObject("mdWrap")
                                    .getAsJsonObject("xmlData")
                                    .getAsJsonObject(section)
                                    .getAsJsonArray(key)
                                    .toString();
                } else if (jsonObject
                        .getAsJsonObject("mets")
                        .getAsJsonArray("dmdSec")
                        .get(counter)
                        .getAsJsonObject()
                        .getAsJsonObject("mdWrap")
                        .getAsJsonObject("xmlData")
                        .getAsJsonObject(section)
                        .get(key)
                        .isJsonPrimitive()) {
                    value =
                            jsonObject
                                    .getAsJsonObject("mets")
                                    .getAsJsonArray("dmdSec")
                                    .get(counter)
                                    .getAsJsonObject()
                                    .getAsJsonObject("mdWrap")
                                    .getAsJsonObject("xmlData")
                                    .getAsJsonObject(section)
                                    .getAsJsonPrimitive(key)
                                    .getAsString();
                }
            }
        }
        return value;
    }
}
