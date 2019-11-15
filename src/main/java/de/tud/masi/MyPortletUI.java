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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.json.XML;
import org.osgi.service.component.annotations.Component;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.vaadin.annotations.Widgetset;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.Page.Styles;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import edu.kit.dama.authorization.entities.GroupId;
import edu.kit.dama.authorization.entities.IAuthorizationContext;
import edu.kit.dama.authorization.entities.Role;
import edu.kit.dama.authorization.entities.UserId;
import edu.kit.dama.authorization.entities.impl.AuthorizationContext;
import edu.kit.dama.authorization.exceptions.UnauthorizedAccessAttemptException;
import edu.kit.dama.mdm.admin.ServiceAccessToken;
import edu.kit.dama.mdm.base.UserData;
import edu.kit.dama.mdm.core.IMetaDataManager;
import edu.kit.dama.mdm.core.MetaDataManagement;
import edu.kit.dama.mdm.dataorganization.impl.jpa.FileNode;
import edu.kit.dama.rest.SimpleRESTContext;
import edu.kit.dama.rest.client.DataManagerPropertiesImpl;
import edu.kit.dama.rest.client.access.impl.SearchRestClient;
import de.tud.masi.components.ClearableComboBox;
import edu.kit.dama.util.Constants;
import edu.kit.dama.util.CryptUtil;
import edu.kit.jcommander.generic.status.CommandStatus;
import edu.kit.jcommander.generic.status.Status;

/**
 * Main search and access area.
 *
 * @author Richard Grunzke
 */

// @Theme("mytheme")
@Widgetset("de.tud.masi.AppWidgetSet")
@Component(
        service = UI.class,
        property = {

            // use this section for production use
            //      "com.liferay.portlet.display-category=category.masi",
            //      "javax.portlet.name=MASiGenGUI",
            //      "javax.portlet.display-name=MASi Community",

            // use this section for development use
            "com.liferay.portlet.display-category=category.testing",
            "javax.portlet.name=MASiGenGUI-DEV",
            "javax.portlet.display-name=MASi Community - Dev",
            "javax.portlet.security-role-ref=power-user,user",
            "com.vaadin.osgi.liferay.portlet-ui=true"
        })
public class MyPortletUI extends UI {
    private static final long serialVersionUID = 1L;

    private String groupOfUser;
    private User user;
    private String searchQuery = null;
    public int entriesPerPage = 10;

    private SimpleRESTContext restContext = null;
    private SearchRestClient searchRestClient = null;
    private DataManagerPropertiesImpl dataManagerProperties;
    private VerticalLayout mainLayout;
    private HorizontalLayout searchRowLayout;
    private VerticalLayout refineLayout;
    private HorizontalLayout statusLayout;
    private HorizontalLayout refineChooseLayout;
    private Label statusLabel = null;
    private Button searchButton;
    private Button discoverButton;
    private Button resetButton;
    private PaginationPanel paginationPanel;
    private String minDate = null;
    private String maxDate = null;
    public String language = null;

    private static final UserId USER = new UserId(Constants.SYSTEM_ADMIN);
    private static final GroupId USERS_GROUP = new GroupId(Constants.SYSTEM_GROUP);

    private int searchResultNumber = 0;
    private final HashMap<String, String> valuesMap = new HashMap<String, String>();
    public final HashMap<String, HashMap<String, String>> objectValuesMap =
            new HashMap<String, HashMap<String, String>>();
    private final JsonParser parser = new JsonParser();
    private ThemeDisplay themeDisplay = null;

    public String siteName = null;
    public final String siteNameCVMA = "Corpus Vitrearum Medii Aevi";
    private Long liferayUserId;

    private String objectIdLocal;

    private ClearableComboBox<String> stateBox;
    private ClearableComboBox<String> cityBox;
    private ClearableComboBox<String> sublocationBox;
    private String stateBoxValue;
    private String cityBoxValue;
    private String sublocationBoxValue;

    private final List<String> resultObjectIDs = new ArrayList<>();
    private List<String> tmpResultObjectIDs = new ArrayList<>();
    private List<String> cityResultObjectIDs = new ArrayList<>();
    private List<String> stateResultObjectIDs = new ArrayList<>();
    private List<String> subLocationResultObjectIDs = new ArrayList<>();
    private final List<String> dateResultObjectIDs = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    private HashMap<String, ArrayList> stateHashMap = new HashMap<String, ArrayList>();

    @SuppressWarnings("rawtypes")
    private HashMap<String, ArrayList> cityHashMap = new HashMap<String, ArrayList>();

    @SuppressWarnings("rawtypes")
    private HashMap<String, ArrayList> sublocationHashMap = new HashMap<String, ArrayList>();

    private HashMap<String, String> valuesMapLocal = new HashMap<String, String>();

    private IMetaDataManager mdmDo = null;

    private PersistentCacheManager resultCacheManager = null;
    private PersistentCacheManager valueCacheManager = null;
    private PersistentCacheManager refineCacheManager = null;
    private PersistentCacheManager refineDateCacheManager = null;

    private Cache<Integer, String> resultCache;
    private Cache<String, HashMap> valueCache;
    private Cache<String, HashMap> refineCache;
    private Cache<String, ArrayList> refineDateCache;

    private long startTime;
    private VaadinRequest localRequest;
    private boolean discover = false;

    public String titleString = null;
    public String subjectString = null;
    public String creatorString = null;
    public String dateString = null;
    public String typeString = null;
    public String publisherString = null;
    public String idString = null;
    public String rightsString = null;
    public String descriptionString = null;
    public String locationString = null;
    public String unknownString = null;
    public String entityString = null;
    public String entityroleString = null;

    /**
     * Provide the Liferay Group ID of current user.
     *
     * @return Liferay Group ID.
     */
    public GroupId getUsersGroup() {
        return USERS_GROUP;
    }

    @Override
    protected void init(VaadinRequest request) {

        UI.getCurrent()
                .setErrorHandler(
                        new DefaultErrorHandler() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void error(com.vaadin.server.ErrorEvent event) {
                                log("Uncaught Exception:");
                                event.getThrowable().printStackTrace();
                            }
                        });

        localRequest = request;
        language = getLanguage();
        themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        liferayUserId = themeDisplay.getUser().getUserId();
        try {
            siteName = themeDisplay.getScopeGroupName();
        } catch (PortalException e1) {
            e1.printStackTrace();
        }

        try {
            user = UserLocalServiceUtil.getUserById(liferayUserId);
        } catch (PortalException | SystemException e2) {
            e2.printStackTrace();
        }

        log("Start GUI");

        // decouple cache init and extendedInit()
        new Thread() {
            public void run() {
                try {
                    openResultCache();
                    closeCache(resultCacheManager);
                    openValueCache();
                    closeCache(valueCacheManager);
                    openRefineCache();
                    closeCache(refineCacheManager);
                    openRefineDateCache();
                    closeCache(refineDateCacheManager);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                extendedInit(request);
            }
        }.start();
        try {
            buildSearchView(null, request);
        } catch (PortalException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Provide the relevant IAuthorizationContext.
     *
     * @return IAuthorizationContext
     */
    public IAuthorizationContext getAuthorizationContext() {
        IAuthorizationContext authContext = AuthorizationContext.factorySystemContext();
        authContext.setUserId(USER);
        authContext.setGroupId(USERS_GROUP);
        authContext.setRoleRestriction(Role.ADMINISTRATOR);
        return authContext;
    }

    /** Built the main search view */
    private void buildSearchView(String pQuery, VaadinRequest request)
            throws PortalException, IOException, InterruptedException {

        // language specific strings
        if (language.equals("Deutsch")) {
            titleString = "Titel";
            subjectString = "Betreff";
            creatorString = "Foto";
            dateString = "Datierung";
            typeString = "Typ";
            publisherString = "Herausgeber";
            idString = "ID";
            rightsString = "Nutzungsbedingungen";
            descriptionString = "Beschreibung";
            locationString = "Standort";
            unknownString = "unbekannt";
            entityString = "Entität";
            entityroleString = "Rolle der Entität";
        } else {
            titleString = "Title";
            subjectString = "Subject";
            creatorString = "Photo";
            dateString = "Age Determination";
            typeString = "Type";
            publisherString = "Publisher";
            idString = "ID";
            rightsString = "Terms of Use";
            descriptionString = "Description";
            locationString = "Location";
            unknownString = "unknown";
            entityString = "Entity";
            entityroleString = "Role of Entity";
        }

        // define main layout
        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setMargin(new MarginInfo(false, false, false, false));
        mainLayout.setStyleName("white");
        setContent(mainLayout);

        // define vertical layout for options and define entries
        VerticalLayout optionLayout = new VerticalLayout();
        optionLayout.setMargin(true);
        List<Integer> entryNumberData = new ArrayList<>();
        entryNumberData.add(5);
        entryNumberData.add(10);
        entryNumberData.add(25);
        List<String> languageData = new ArrayList<>();
        languageData.add("Deutsch");
        languageData.add("English");

        NativeSelect<Integer> entryNumberDropDownList;
        NativeSelect<String> languageDropDownList;

        TextField searchField = new TextField();
        PopupView view;
        if (language.equals("Deutsch")) {
            searchButton = new Button("Suche");
            discoverButton = new Button("Entdecken");
            searchField.setPlaceholder("Suche nach ...");
            view = new PopupView("Optionen", optionLayout);
            entryNumberDropDownList = new NativeSelect<>("Einträge pro Seite", entryNumberData);
            languageDropDownList = new NativeSelect<>("Sprache", languageData);
        } else {
            searchButton = new Button("Search");
            discoverButton = new Button("Discover");
            searchField.setPlaceholder("Search for ...");
            view = new PopupView("Options", optionLayout);
            entryNumberDropDownList = new NativeSelect<>("Entries per page", entryNumberData);
            languageDropDownList = new NativeSelect<>("Language", languageData);
        }

        // trigger searchButton by pressing enter
        searchButton.setClickShortcut(KeyCode.ENTER);

        // define width and style of searchField
        searchField.setWidth("400px");
        searchField.setStyleName("white");

        // define horizontal layout to include searchField and searchButton and define alignment for
        // each
        searchRowLayout = new HorizontalLayout();
        searchRowLayout.setStyleName("white");
        searchRowLayout.addComponent(searchField);
        searchRowLayout.setComponentAlignment(searchField, Alignment.MIDDLE_CENTER);
        searchRowLayout.addComponent(searchButton);
        searchRowLayout.setComponentAlignment(searchButton, Alignment.MIDDLE_CENTER);
        searchRowLayout.setStyleName("searchrow");

        // distinguish between community use cases, if CVMA, then add additional discover button
        if (siteName.equals(siteNameCVMA)) {
            searchRowLayout.addComponent(discoverButton);
            searchRowLayout.setComponentAlignment(discoverButton, Alignment.MIDDLE_CENTER);
        }

        Styles styles = Page.getCurrent().getStyles();
        styles.add(".searchrow { margin auto; text-align: center; width: 50%; !important}");

        // enable to select number of search result visible of one page and configure it
        entryNumberDropDownList.setWidth("115px");
        entryNumberDropDownList.setEmptySelectionAllowed(false);
        entryNumberDropDownList.setSelectedItem(entryNumberData.get(1));
        entryNumberDropDownList.addValueChangeListener(
                event -> {
                    entriesPerPage = event.getValue();
                    log("Click on selectEntryNumber with value: " + entriesPerPage);
                    try {
                        paginationPanel.setAllEntries(resultObjectIDs);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
        optionLayout.addComponent(entryNumberDropDownList);
        optionLayout.setComponentAlignment(entryNumberDropDownList, Alignment.TOP_CENTER);

        // enable to select language and configure it
        languageDropDownList.setWidth("115px");
        languageDropDownList.setEmptySelectionAllowed(false);
        languageDropDownList.setSelectedItem(language);
        languageDropDownList.addValueChangeListener(
                event -> {
                    language = event.getValue();
                    log("Click on selectLanguage with value: " + language);
                    try {
                        buildSearchView(null, request);
                    } catch (IOException | PortalException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
        optionLayout.addComponent(languageDropDownList);
        optionLayout.setComponentAlignment(languageDropDownList, Alignment.TOP_CENTER);

        // hide options window if mouse if moved away
        view.setHideOnMouseOut(true);

        // add options window to search row and align it
        searchRowLayout.addComponent(view);
        searchRowLayout.setComponentAlignment(view, Alignment.MIDDLE_LEFT);

        // add search row to main layout and align it
        mainLayout.addComponent(searchRowLayout);
        mainLayout.setComponentAlignment(searchRowLayout, Alignment.MIDDLE_CENTER);

        // create status layout, configure it and add it to main layout and align it
        statusLayout = new HorizontalLayout();
        mainLayout.addComponent(statusLayout);
        mainLayout.setComponentAlignment(statusLayout, Alignment.MIDDLE_CENTER);

        // distinguish between community use cases, if CVMA, then add layout for refining search
        // results
        if (siteName.equals(siteNameCVMA)) {
            refineLayout = new VerticalLayout();
            mainLayout.addComponent(refineLayout);
            mainLayout.setComponentAlignment(refineLayout, Alignment.BOTTOM_CENTER);
        }

        // create layout for search results and add it to the main layout
        paginationPanel = new PaginationPanel(this);
        paginationPanel.setSizeFull();
        paginationPanel.setStyleName("white");
        paginationPanel.setAllEntries(new ArrayList<String>());
        mainLayout.addComponent(paginationPanel);
        mainLayout.setComponentAlignment(paginationPanel, Alignment.TOP_CENTER);

        // define action that are triggered when the search button is clicked
        searchButton.addClickListener(
                event -> {

                    // a search was triggered, not a discover operation
                    discover = false;

                    // close all sub windows start new search context
                    closeSubWindows();

                    log("Click on search");

                    // reset and get new search query
                    searchQuery = null;
                    searchQuery = searchField.getValue();

                    String text;
                    if (searchField.getValue() != null && !searchField.getValue().isEmpty()) {
                        while (searchRestClient == null) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            } catch (InterruptedException e2) {
                                e2.printStackTrace();
                            }
                        }

                        // remove special character from search query, except Deutsch umlauts
                        searchQuery = searchQuery.replaceAll("[^äÄöÖüÜß\\w\\s]", " ");

                        // convert string to array string
                        String[] searchTerms = searchQuery.split(" ");

                        if (searchQuery.length() >= 3) {
                            text = "Searching for \"" + searchQuery + "\"";
                            log(text);

                            // issue search command with searchData method with searchTerms
                            try {
                                searchData(null, null, searchTerms).getStatusMessage();
                            } catch (FileNotFoundException | InterruptedException e1) {
                                e1.printStackTrace();
                            }

                            // if there are search results
                            if (searchResultNumber > 0) {
                                // enable the user to see number of search results
                                if (language.equals("Deutsch")) {
                                    text = "Resultate: " + searchResultNumber;
                                } else {
                                    text = "Results: " + searchResultNumber;
                                }
                                statusLabel = new Label(text, ContentMode.HTML);
                                log(text);

                                // create refine layout if CVMA is active
                                try {
                                    if (siteName.equals(siteNameCVMA)) {
                                        refine();
                                    }
                                } catch (IOException
                                        | UnauthorizedAccessAttemptException
                                        | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else // if there are no search results, then display that and clear
                            {
                                // enable the user to see the actual search query utilized also in
                                // case of no search results
                                if (language.equals("Deutsch")) {
                                    text = "Keine Resultate gefunden.";
                                } else {
                                    text = "No results found.";
                                }
                                statusLabel = new Label(text, ContentMode.HTML);
                                log(text);
                                try {

                                    if (siteName.equals(siteNameCVMA)) {
                                        refineLayout.setMargin(false);
                                        refineLayout.removeAllComponents();
                                    }

                                    tmpResultObjectIDs.clear();
                                    resultObjectIDs.clear();
                                    paginationPanel.setAllEntries(new ArrayList<String>());
                                } catch (IOException | InterruptedException e3) {
                                    e3.printStackTrace();
                                }
                            }

                            // clear statusLayout and add statusLaber to display number of search
                            // results, then resets searchTeams
                            statusLayout.removeAllComponents();
                            statusLayout.addComponent(statusLabel);
                            statusLayout.setComponentAlignment(
                                    statusLabel, Alignment.MIDDLE_CENTER);
                            searchTerms = null;
                        } else {
                            if (language.equals("Deutsch")) {
                                text = "Suchanfrage zu kurz, muss mindestens 3 Zeichen lang sein.";
                            } else {
                                text = "Search query too short, must be at least 3 characters.";
                            }
                            statusLabel = new Label(text, ContentMode.HTML);
                            log(text);
                            statusLayout.removeAllComponents();
                            statusLayout.addComponent(statusLabel);
                            statusLayout.setComponentAlignment(
                                    statusLabel, Alignment.MIDDLE_CENTER);
                            try {
                                if (siteName.equals(siteNameCVMA)) {
                                    refineLayout.setMargin(false);
                                    refineLayout.removeAllComponents();
                                }
                                tmpResultObjectIDs.clear();
                                resultObjectIDs.clear();
                                paginationPanel.setAllEntries(new ArrayList<String>());
                            } catch (IOException | InterruptedException e4) {
                                e4.printStackTrace();
                            }
                        }
                    } else {
                        statusLayout.removeAllComponents();
                        try {
                            if (siteName.equals(siteNameCVMA)) {
                                refineLayout.setMargin(false);
                                refineLayout.removeAllComponents();
                            }
                            tmpResultObjectIDs.clear();
                            resultObjectIDs.clear();
                            paginationPanel.setAllEntries(new ArrayList<String>());
                        } catch (IOException | InterruptedException e5) {
                            e5.printStackTrace();
                        }
                    }
                });

        // define action that are triggered when the discover button is clicked
        discoverButton.addClickListener(
                event -> {

                    // a discover operation was triggered
                    discover = true;
                    log("Click on discover");

                    // close all sub windows start new search context
                    closeSubWindows();

                    // reset and set dummy search query to show all results
                    searchField.setValue("");
                    searchQuery = null;
                    String text;
                    if (siteName.equals(siteNameCVMA)) {
                        searchQuery = "Glasmalerei";
                    }

                    while (searchRestClient == null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                    }

                    // convert string to array string
                    String[] searchTerms = searchQuery.split(" ");

                    // issue search command with searchData method with searchTerms
                    try {
                        searchData(null, null, searchTerms).getStatusMessage();
                    } catch (FileNotFoundException | InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    searchTerms = null;

                    // display number of search results
                    if (searchResultNumber > 0) {
                        if (language.equals("Deutsch")) {
                            text = "Resultate: " + searchResultNumber;
                        } else {
                            text = "Results: " + searchResultNumber;
                        }
                        statusLabel = new Label(text, ContentMode.HTML);
                        log(text);
                    } else {
                        text = "No results found within " + siteNameCVMA + ".";
                        statusLabel = new Label(text, ContentMode.HTML);
                        log(text);
                        try {
                            paginationPanel.setAllEntries(new ArrayList<String>());
                        } catch (IOException | InterruptedException e3) {
                            e3.printStackTrace();
                        }
                    }
                    statusLayout.removeAllComponents();
                    statusLayout.addComponent(statusLabel);
                    statusLayout.setComponentAlignment(statusLabel, Alignment.MIDDLE_CENTER);

                    // display refine layout if CVMA
                    try {
                        if (siteName.equals(siteNameCVMA)) {
                            refine();
                        }
                    } catch (IOException
                            | UnauthorizedAccessAttemptException
                            | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * perform search or get search result from cache
     *
     * @param type not used
     * @param index not used
     * @param term Search terms split up in string array
     * @return
     * @throws FileNotFoundException
     * @throws InterruptedException
     */
    private CommandStatus searchData(List<String> type, List<String> index, String[] term)
            throws FileNotFoundException, InterruptedException {
        CommandStatus returnStatus = null;
        try {
            // clear specific lists
            cityResultObjectIDs.clear();
            stateResultObjectIDs.clear();
            subLocationResultObjectIDs.clear();
            dateResultObjectIDs.clear();

            // reset refinebox values
            stateBoxValue = null;
            cityBoxValue = null;
            sublocationBoxValue = null;

            // reset search result number
            searchResultNumber = 0;

            if (dataManagerProperties != null) {
                String searchResult2;
                String[] parts = null;
                long startTime2 = System.nanoTime();

                /*
                 to optimize response time, cache search results for CVMA Open Access showcase for
                 adapting this to other use cases, beware of access rights and varying search
                 results with more non-static data, introduce time to life, as search result may
                 change
                */
                if (siteName.equals(siteNameCVMA)) {
                    openResultCache();
                    // if result already cached, get result form cache otherwise, perform search and
                    // put result into cache
                    if (resultCache.containsKey(Arrays.deepHashCode(term))) {
                        searchResult2 = resultCache.get(Arrays.deepHashCode(term));
                    } else {
                        searchResult2 =
                                searchRestClient.getSearchResultList(
                                        groupOfUser, null, null, term, 10000, restContext, true);
                        resultCache.put(Arrays.deepHashCode(term), searchResult2);
                    }
                    closeCache(resultCacheManager);
                } else {
                    // for non-CVMA use cases, perform search
                    searchResult2 =
                            searchRestClient.getSearchResultList(
                                    groupOfUser, null, null, term, 10000, restContext, true);
                }

                long estimatedTime = System.nanoTime() - startTime2;
                // parts = searchResult2.split("digitalObjectId");
                // convert roughly to ms
                // String time = String.valueOf(estimatedTime);
                // time = time.substring(0, time.length() - 6);

                if (searchResult2 != null
                        && !searchResult2.isEmpty()
                        && !searchResult2.equals("{}")) {
                    // clear list and maps for new search
                    resultObjectIDs.clear();
                    valuesMap.clear();

                    // convert search result in form of list of object IDs (as json array)
                    // to list of object IDs (resultObjectIDs)
                    fillresultObjectList(searchResult2);

                    // determine and set number of search results
                    searchResultNumber = resultObjectIDs.size();

                    // set entries in pagination panel with current list of object IDs
                    paginationPanel.setAllEntries(resultObjectIDs);
                }
                returnStatus = new CommandStatus(Status.SUCCESSFUL);
            }
        } catch (IllegalArgumentException | IOException iae) {
            returnStatus = new CommandStatus(Status.FAILED);
        }
        return returnStatus;
    }

    private void extendedInit(VaadinRequest request) {

        String restAccessKey = null;
        String restAccessSecret = null;

        // get properties from KIT DM config
        dataManagerProperties = DataManagerPropertiesImpl.getDefaultInstance();

        // Gather key, secret and group for REST access
        try {
            // if CVMA, set values from properties file
            // else (logged in or non-CVMA access) dynamically get values
            if (siteName.equals(siteNameCVMA)) {
                restAccessKey = dataManagerProperties.getAccessKey();
                restAccessSecret = dataManagerProperties.getAccessSecret();
                groupOfUser = "CVMA";
            } else {
                // build MetaDataManagement object to access data in KIT DM database
                log("Build MetaDataManagement object");
                IMetaDataManager mdm =
                        MetaDataManagement.getMetaDataManagement().getMetaDataManager();
                log("Finished building MetaDataManagement object");

                // set auth context
                mdm.setAuthorizationContext(getAuthorizationContext());

                // get user object from KIT DM database via mail address of currently logged in user

                UserData userObjectFromKITDMDB =
                        mdm.findSingleResult(
                                "Select u FROM UserData u WHERE u.email=?1",
                                new Object[] {user.getEmailAddress()},
                                UserData.class);

                // get rest token from KIT DM database
                ServiceAccessToken restAccessTokenFromKITDMDB =
                        mdm.findSingleResult(
                                "Select t FROM ServiceAccessToken t WHERE t.userId=?1 AND t.serviceId='restServiceAccess'",
                                new Object[] {userObjectFromKITDMDB.getDistinguishedName()},
                                ServiceAccessToken.class);

                // get access key form token and set it
                restAccessKey = restAccessTokenFromKITDMDB.getTokenKey();

                // get group object from KIT DM database
                edu.kit.dama.mdm.admin.UserGroup groupResult =
                        mdm.findSingleResult(
                                "Select g FROM UserGroup g WHERE g.description=?1",
                                new Object[] {themeDisplay.getScopeGroupName()},
                                edu.kit.dama.mdm.admin.UserGroup.class);

                // get group from group object and set it
                groupOfUser = groupResult.getGroupId();

                // get access secret from token, decrypt it and set it
                try {
                    restAccessSecret =
                            CryptUtil.getSingleton()
                                    .decrypt(restAccessTokenFromKITDMDB.getTokenSecret());
                } catch (InvalidKeyException
                        | NoSuchAlgorithmException
                        | NoSuchPaddingException
                        | ShortBufferException
                        | IllegalBlockSizeException
                        | BadPaddingException e) {
                    e.printStackTrace();
                }

                // close MetaDataManagementObject
                mdm.close();

                log("After DB access");
            }

        } catch (UnauthorizedAccessAttemptException | PortalException e2) {
            e2.printStackTrace();
        }

        // set context for rest access
        restContext = new SimpleRESTContext(restAccessKey, restAccessSecret);

        // create search rest client with rest URL from properties and with context
        searchRestClient = new SearchRestClient(dataManagerProperties.getRestUrl(), restContext);

        log("Search ready ");
    }

    /**
     * convert search result in form of list of object IDs (as json array) to list of object IDs
     * (resultObjectIDs)
     *
     * @param searchResultLocal
     */
    private void fillresultObjectList(String searchResultLocal) {
        JsonArray array = parser.parse(searchResultLocal).getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            resultObjectIDs.add(
                    array.get(i)
                            .getAsJsonObject()
                            .getAsJsonPrimitive("digitalObjectId")
                            .getAsString());
        }
        // if discover clicked, randomly shuffle result list
        if (discover) {
            Collections.shuffle(resultObjectIDs);
        }
    }

    /**
     * define and set refine layout init comboboxes
     *
     * @throws IOException
     * @throws UnauthorizedAccessAttemptException
     * @throws InterruptedException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void refine()
            throws IOException, UnauthorizedAccessAttemptException, InterruptedException {
        openValueCache();
        openRefineCache();
        refineLayout.removeAllComponents();
        refineLayout.setStyleName("white");
        refineLayout.setMargin(new MarginInfo(true, false, false, false));
        Label refineLabel;

        if (language.equals("Deutsch")) {
            refineLabel = new Label("Suchergebnisse verfeinern");
        } else {
            refineLabel = new Label("Refine search results");
        }

        refineLayout.addComponent(refineLabel);
        refineLayout.setComponentAlignment(refineLabel, Alignment.MIDDLE_CENTER);

        // init layout to hold actual comboboxes to refine search
        refineChooseLayout = new HorizontalLayout();

        // init and fill hashmap for state combobox
        stateHashMap = new HashMap<String, ArrayList>();
        fillHashMap(stateHashMap, resultObjectIDs, "Iptc4xmpExt:ProvinceState");

        // init state combobox (fixed version that is clearable)
        stateBox = new ClearableComboBox<String>(null);

        // set small style
        stateBox.addStyleName(ValoTheme.COMBOBOX_SMALL);

        // set either German or English caption set content and caption of combo box
        if (language.equals("Deutsch")) {
            setBoxItems(stateBox, "Bundesland", stateHashMap);
        } else {
            setBoxItems(stateBox, "State", stateHashMap);
        }

        // add combobox to refine choose layout
        refineChooseLayout.addComponent(stateBox);
        refineChooseLayout.setComponentAlignment(stateBox, Alignment.MIDDLE_CENTER);

        // add listener that is triggered when the value is changed
        stateBox.addValueChangeListener(
                event -> {
                    try {
                        // if value not empty, then
                        // 	set new box value to based on current event
                        // 	set current list of object IDs as intersetion between list of result IDs
                        // 		and respective hashmap
                        // otherwise clear
                        if (event != null && event.getValue() != null) {
                            stateBoxValue = event.getValue();
                            stateResultObjectIDs =
                                    (List<String>)
                                            CollectionUtils.intersection(
                                                    resultObjectIDs,
                                                    stateHashMap.get(event.getValue()));
                        } else {
                            stateResultObjectIDs.clear();
                            stateBoxValue = null;
                        }
                        handleBox("Iptc4xmpExt:ProvinceState", stateBoxValue);
                    } catch (IOException
                            | UnauthorizedAccessAttemptException
                            | InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        // init and fill hashmap for city combobox
        cityHashMap = new HashMap<String, ArrayList>();
        fillHashMap(cityHashMap, resultObjectIDs, "Iptc4xmpExt:City");

        // init city combobox (fixed version that is clearable)
        cityBox = new ClearableComboBox<String>(null);

        // set small style
        cityBox.addStyleName(ValoTheme.COMBOBOX_SMALL);

        // set either German or English caption set content and caption of combo box
        if (language.equals("Deutsch")) {
            setBoxItems(cityBox, "Stadt", cityHashMap);
        } else {
            setBoxItems(cityBox, "City", cityHashMap);
        }

        // add combobox to refine choose layout
        refineChooseLayout.addComponent(cityBox);
        refineChooseLayout.setComponentAlignment(cityBox, Alignment.MIDDLE_CENTER);

        // add listener that is triggered when the value is changed
        cityBox.addValueChangeListener(
                event -> {
                    try {
                        // if value not empty, then
                        // 	set new box value to based on current event
                        // 	set current list of object IDs as intersetion between list of result IDs
                        // 		and respective hashmap
                        // otherwise clear
                        if (event != null && event.getValue() != null) {
                            cityBoxValue = event.getValue();
                            cityResultObjectIDs =
                                    (List<String>)
                                            CollectionUtils.intersection(
                                                    resultObjectIDs,
                                                    cityHashMap.get(event.getValue()));
                        } else {
                            cityResultObjectIDs.clear();
                            cityBoxValue = null;
                        }
                        handleBox("Iptc4xmpExt:City", cityBoxValue);
                    } catch (IOException
                            | UnauthorizedAccessAttemptException
                            | InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        // init and fill hashmap for sublocation combobox
        sublocationHashMap = new HashMap<String, ArrayList>();
        fillHashMap(sublocationHashMap, resultObjectIDs, "Iptc4xmpExt:Sublocation");

        // init sublocation combobox (fixed version that is clearable)
        sublocationBox = new ClearableComboBox<String>(null);

        // set small style
        sublocationBox.addStyleName(ValoTheme.COMBOBOX_SMALL);

        // set either German or English caption set content and caption of combo box
        if (language.equals("Deutsch")) {
            setBoxItems(sublocationBox, "Gebäude", sublocationHashMap);
        } else {
            setBoxItems(sublocationBox, "Building", sublocationHashMap);
        }

        // add combobox to refine choose layout
        refineChooseLayout.addComponent(sublocationBox);
        refineChooseLayout.setComponentAlignment(sublocationBox, Alignment.MIDDLE_CENTER);

        // add listener that is triggered when the value is changed
        sublocationBox.addValueChangeListener(
                event -> {
                    try {
                        // if value not empty, then
                        // 	set new box value to based on current event
                        // 	set current list of object IDs as intersetion between list of result IDs
                        // 		and respective hashmap
                        // otherwise clear
                        if (event != null && event.getValue() != null) {
                            sublocationBoxValue = event.getValue();
                            subLocationResultObjectIDs =
                                    (List<String>)
                                            CollectionUtils.intersection(
                                                    resultObjectIDs,
                                                    sublocationHashMap.get(event.getValue()));
                        } else {
                            subLocationResultObjectIDs.clear();
                            sublocationBoxValue = null;
                        }
                        handleBox("Iptc4xmpExt:Sublocation", sublocationBoxValue);
                    } catch (IOException
                            | UnauthorizedAccessAttemptException
                            | InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        minDate = "0";
        maxDate = "2000";

        // deactivate Date Refinement
        //      	lowerDateField = new TextField("Start Date");
        //      	lowerDateField.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        //      	lowerDateField.setWidth("70px");
        //      	lowerDateField.setMaxLength(4);
        //      	lowerDateField.setResponsive(true);
        //      	lowerDateField.setValueChangeMode(ValueChangeMode.LAZY);
        //      	lowerDateField.setValueChangeTimeout(1000);
        //      	lowerDateField.addValueChangeListener(event -> {
        //      		String value = event.getValue();
        //      		if ( value.matches("[0-9]+") )
        //      		{
        //      			dateResultObjectIDs = (List<String>)
        // CollectionUtils.intersection(resultObjectIDs, dateList);
        //      			try {
        ////					handleBox("lowerDateField", "lowerDateField");
        //					handleBoxDate("lowerDateField", "lowerDateField");
        //				} catch (UnauthorizedAccessAttemptException | IOException e) { e.printStackTrace(); }
        //
        //      		}
        //      		else
        //      		{
        //          		value = value.replaceAll("[^0-9]+", "");
        //          		if ( value.length() == 0 || value == null ) {
        //          			value = String.valueOf(min);
        //          		}
        //          		value = new Integer(value).toString();
        //          		if ( Integer.valueOf(value) < Integer.valueOf(min) ) {
        //          			value = min;
        //          		}
        //          		if ( Integer.valueOf(value) > Integer.valueOf(upperDateField.getValue()) ) {
        //          			value = String.valueOf(upperDateField.getValue());
        //          		}
        //          		lowerDateField.setValue(value);
        //      		}
        //        });
        //      	refineChooseLayout.addComponent(lowerDateField);
        //
        //
        //      	upperDateField = new TextField("End Date");
        //      	upperDateField.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        //      	upperDateField.setWidth("70px");
        //      	upperDateField.setMaxLength(4);
        //  		upperDateField.setValueChangeMode(ValueChangeMode.LAZY);
        //  		upperDateField.setValueChangeTimeout(1000);
        //      	upperDateField.addValueChangeListener(event -> {
        //      		String value = event.getValue();
        //      		if ( value.matches("[0-9]+") )
        //      		{
        //      			dateResultObjectIDs = (List<String>)
        // CollectionUtils.intersection(resultObjectIDs, dateList);
        //      			try {
        ////					handleBox("upperDateField", "upperDateField");
        //					handleBoxDate("upperDateField", "upperDateField");
        //				} catch (UnauthorizedAccessAttemptException | IOException e) { e.printStackTrace(); }
        //
        //      		}
        //      		else
        //      		{
        //          		value = value.replaceAll("[^0-9]+", "");
        //          		if ( value.length() == 0 || value == null ) {
        //          			value = max;
        //          		}
        //          		value = new Integer(value).toString();
        //          		if ( Integer.valueOf(value) > Integer.valueOf(max) ) {
        //          			value = String.valueOf(max);
        //          		}
        //          		if ( Integer.valueOf(value) < Integer.valueOf(lowerDateField.getValue()) ) {
        //          			value = String.valueOf(lowerDateField.getValue());
        //          		}
        //          		upperDateField.setValue(value);
        //      		}
        //        });
        //      	refineChooseLayout.addComponent(upperDateField);

        // deactivate Date Refinement
        //      	dateList = new ArrayList();
        //      	fillDateList(dateList, resultObjectIDs, min, max);
        //      	lowerDateField.setValue(min);
        //      	upperDateField.setValue(max);

        if (language.equals("Deutsch")) {
            resetButton = new Button("Zurücksetzen");
        } else {
            resetButton = new Button("Reset");
        }
        resetButton.addStyleName(ValoTheme.BUTTON_SMALL);
        refineChooseLayout.addComponent(resetButton);
        refineChooseLayout.setComponentAlignment(resetButton, Alignment.BOTTOM_CENTER);
        resetButton.addClickListener(
                event -> {
                    log("Reset Button Clicked");
                    stateBox.setValue(null);
                    cityBox.setValue(null);
                    sublocationBox.setValue(null);

                    // deactivate Date Refinement
                    //          		lowerDateField.setValue(min);
                    //          		upperDateField.setValue(max);

                    closeSubWindows();
                });

        refineLayout.addComponent(refineChooseLayout);
        refineLayout.setComponentAlignment(refineChooseLayout, Alignment.MIDDLE_CENTER);

        closeCache(refineCacheManager);
        closeCache(valueCacheManager);
    }

    /**
     * fills given HashMaps (one for state, city and sublocation) with keys (either a state, a city
     * or a sublocation) and fitting values (list of object IDs) ie: key: Nürnberg, values as object
     * IDs: ID1, ID2 meaning: every image (ID1, ID2) that resides in Nürnberg based on list of given
     * object IDs (either complete search results or already refined list)
     *
     * @param localHashMap
     * @param localResultObjectIDs
     * @param localField
     * @throws IOException
     * @throws UnauthorizedAccessAttemptException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fillHashMap(
            HashMap<String, ArrayList> localHashMap,
            List<String> localResultObjectIDs,
            String localField)
            throws IOException, UnauthorizedAccessAttemptException {
        long ltmp = 0;
        int count = localResultObjectIDs.size();
        int misses = 0;
        int hits = 0;
        startTime = System.currentTimeMillis();

        ArrayList<String> tempList;

        // create and fill list of strings for all currently relevant object IDs
        List<String> idList = new ArrayList<String>(localResultObjectIDs);

        // sort list display in order
        Collections.sort(idList);

        // create local key as unique key with name of field and hash of the content the object ID
        // list
        String localKey = localField + Arrays.deepHashCode(idList.toArray());

        // if unique local key already exists, get corresponding list from refineCache
        if (refineCache.containsKey(localKey)) {
            localHashMap.putAll(refineCache.get(localKey));
            // ltmp = (System.currentTimeMillis() - startTime);

            // if unique local key doesn't already exists, create list and put into refineCache
        } else {
            for (String localResultObjectID : localResultObjectIDs) {
                objectIdLocal = localResultObjectID;
                if (!valueCache.containsKey(objectIdLocal)) {
                    fillObjectValuesMap();
                    valueCache.put(objectIdLocal, valuesMapLocal);
                    misses = misses + 1;
                } else {
                    hits = hits + 1;
                    String valueTemp = (String) valueCache.get(objectIdLocal).get(localField);
                    if (valueTemp != null) {
                        if (localHashMap.containsKey(valueTemp)) {
                            tempList = localHashMap.get(valueTemp);
                            if (!tempList.contains(objectIdLocal)) {
                                tempList.add(objectIdLocal);
                                localHashMap.put(valueTemp, tempList);
                            }
                        } else {
                            tempList = new ArrayList<>();
                            tempList.add(objectIdLocal);
                            localHashMap.put(valueTemp, tempList);
                        }
                    }
                }
            }
            refineCache.put(localKey, localHashMap);
        }
    }

    // deactivate Date Refinement

    /*
     fills given list for date based on list of given object IDs

     @param localDateList
     @param localResultObjectIDs
     @param localRangeStart
     @param localRangeEnd
     @throws IOException
     @throws UnauthorizedAccessAttemptException
     @throws InterruptedException
    */
    //     @SuppressWarnings({ "unchecked", "rawtypes" })
    //     private void fillDateList(ArrayList localDateList,
    //    		 				List<String> localResultObjectIDs,
    //    		 				String localRangeStart,
    //    		 				String localRangeEnd) throws IOException, UnauthorizedAccessAttemptException,
    // InterruptedException {
    //    	 long ltmp = 0;
    //    	 minDate = "3000";
    //    	 maxDate = "0";
    //    	 startTime = System.currentTimeMillis();
    //    	 List<String> tempList = new ArrayList<String>();
    //    	 tempList.addAll(localResultObjectIDs);
    //    	 Collections.sort(tempList);
    //    	 String localKey =
    // localRangeStart+"-"+localRangeEnd+String.valueOf(Arrays.deepHashCode(tempList.toArray()));
    //    	 openRefineDateCache();
    //
    //    	 if ( refineDateCache.containsKey(localKey) )
    //    	 {
    //    		 localDateList.addAll(refineDateCache.get(localKey));
    //    		 ltmp = (System.currentTimeMillis() - startTime);
    //
    //    		 if ( refineDateCache.containsKey(localKey+"minMax") ) {
    //    			 minDate = refineDateCache.get(localKey+"minMax").get(0).toString();
    //    			 maxDate = refineDateCache.get(localKey+"minMax").get(1).toString();
    //    		 } else {
    //
    //    			 fillAndFindAndSetMinMax(localDateList,
    //    						localResultObjectIDs,
    //    						localRangeStart,
    //    						localRangeEnd);
    //    		 }
    //    	 } else {
    //    		 fillAndFindAndSetMinMax(localDateList,
    //						localResultObjectIDs,
    //						localRangeStart,
    //						localRangeEnd);
    //    		 refineDateCache.put(localKey, localDateList);
    //    		 ArrayList minMaxList = new ArrayList();
    //    		 minMaxList.add(minDate);
    //    		 minMaxList.add(maxDate);
    //    		 Collections.sort(minMaxList);
    //    		 refineDateCache.put(localKey+"minMax", minMaxList);
    //    	 }
    //    	 closeCache(refineDateCacheManager);
    //     }

    /**
     * Open cache for search results for CVMA Open Access showcase
     *
     * @throws InterruptedException
     */
    private void openResultCache() throws InterruptedException {
        closeCache(resultCacheManager);
        String cacheName = "resultCache";
        String storePath = "/home/cache/" + cacheName;
        waitForFreeFileLock(storePath);
        boolean OverlappingFileLockExceptionThrown = false;
        int counter = 0;
        do {
            if (OverlappingFileLockExceptionThrown) {
                Random ran = new Random();
                int sleepTime = ran.nextInt(150) + 50;
                TimeUnit.MILLISECONDS.sleep(sleepTime);
                counter = counter + 1;
            }
            OverlappingFileLockExceptionThrown = false;
            try {
                resultCacheManager =
                        CacheManagerBuilder.newCacheManagerBuilder()
                                .with(CacheManagerBuilder.persistence(storePath))
                                .withCache(
                                        cacheName,
                                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                                Integer.class,
                                                String.class,
                                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                        .heap(100, EntryUnit.ENTRIES)
                                                        .offheap(1, MemoryUnit.MB)
                                                        .disk(1000, MemoryUnit.MB, true)))
                                .build(true);
            } catch (Exception e1) {
                OverlappingFileLockExceptionThrown = true;
                e1.printStackTrace();
            }
        } while (OverlappingFileLockExceptionThrown);

        if (counter > 0) {
            log("creation of " + cacheName + " retriggered " + counter + " times");
        }
        resultCache = resultCacheManager.getCache(cacheName, Integer.class, String.class);
        fixCacheTimestamps(storePath, cacheName);
    }

    /**
     * open cache for values for individual object IDs
     *
     * @throws InterruptedException
     */
    private void openValueCache() throws InterruptedException {
        closeCache(valueCacheManager);
        String cacheName = "valueCache";
        String storePath = "/home/cache/" + cacheName;
        waitForFreeFileLock(storePath);
        boolean OverlappingFileLockExceptionThrown = false;
        int counter = 0;
        do {
            if (OverlappingFileLockExceptionThrown) {
                Random ran = new Random();
                int sleepTime = ran.nextInt(150) + 50;
                TimeUnit.MILLISECONDS.sleep(sleepTime);
                counter = counter + 1;
            }
            OverlappingFileLockExceptionThrown = false;
            try {
                valueCacheManager =
                        CacheManagerBuilder.newCacheManagerBuilder()
                                .with(CacheManagerBuilder.persistence(storePath))
                                .withCache(
                                        cacheName,
                                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                                String.class,
                                                HashMap.class,
                                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                        .heap(100, EntryUnit.ENTRIES)
                                                        .offheap(1, MemoryUnit.MB)
                                                        .disk(1000, MemoryUnit.MB, true)))
                                .build(true);
            } catch (Exception e1) {
                OverlappingFileLockExceptionThrown = true;
            }
        } while (OverlappingFileLockExceptionThrown);
        if (counter > 0) {
            log("creation of " + cacheName + " retriggered " + counter + " times");
        }
        valueCache = valueCacheManager.getCache(cacheName, String.class, HashMap.class);
        fixCacheTimestamps(storePath, cacheName);
    }

    /**
     * open cache for refinements
     *
     * @throws InterruptedException
     */
    private void openRefineCache() throws InterruptedException {
        closeCache(refineCacheManager);
        String cacheName = "refineCache";
        String storePath = "/home/cache/" + cacheName;
        waitForFreeFileLock(storePath);
        boolean OverlappingFileLockExceptionThrown = false;
        int counter = 0;
        do {
            if (OverlappingFileLockExceptionThrown) {
                Random ran = new Random();
                int sleepTime = ran.nextInt(150) + 50;
                TimeUnit.MILLISECONDS.sleep(sleepTime);
                counter = counter + 1;
            }
            OverlappingFileLockExceptionThrown = false;
            try {
                refineCacheManager =
                        CacheManagerBuilder.newCacheManagerBuilder()
                                .with(CacheManagerBuilder.persistence(storePath))
                                .withCache(
                                        cacheName,
                                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                                String.class,
                                                HashMap.class,
                                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                        .heap(100, EntryUnit.ENTRIES)
                                                        .offheap(1, MemoryUnit.MB)
                                                        .disk(1000, MemoryUnit.MB, true)))
                                .build(true);
            } catch (Exception e1) {
                OverlappingFileLockExceptionThrown = true;
            }
        } while (OverlappingFileLockExceptionThrown);
        if (counter > 0) {
            log("creation of " + cacheName + " retriggered " + counter + " times");
        }
        refineCache = refineCacheManager.getCache(cacheName, String.class, HashMap.class);
        fixCacheTimestamps(storePath, cacheName);
    }

    /**
     * open cache for date refinement
     *
     * @throws InterruptedException
     */
    private void openRefineDateCache() throws InterruptedException {
        closeCache(refineDateCacheManager);
        String cacheName = "refineDateCache";
        String storePath = "/home/cache/" + cacheName;
        waitForFreeFileLock(storePath);
        boolean OverlappingFileLockExceptionThrown = false;
        int counter = 0;
        do {
            if (OverlappingFileLockExceptionThrown) {
                Random ran = new Random();
                int sleepTime = ran.nextInt(150) + 50;
                TimeUnit.MILLISECONDS.sleep(sleepTime);
                counter = counter + 1;
            }
            OverlappingFileLockExceptionThrown = false;
            try {
                refineDateCacheManager =
                        CacheManagerBuilder.newCacheManagerBuilder()
                                .with(CacheManagerBuilder.persistence(storePath))
                                .withCache(
                                        cacheName,
                                        CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                                String.class,
                                                ArrayList.class,
                                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                                        .heap(100, EntryUnit.ENTRIES)
                                                        .offheap(1, MemoryUnit.MB)
                                                        .disk(1000, MemoryUnit.MB, true)))
                                .build(true);
            } catch (Exception e1) {
                OverlappingFileLockExceptionThrown = true;
            }
        } while (OverlappingFileLockExceptionThrown);
        if (counter > 0) {
            log("creation of " + cacheName + " retriggered " + counter + " times");
        }
        refineDateCache = refineDateCacheManager.getCache(cacheName, String.class, ArrayList.class);
        fixCacheTimestamps(storePath, cacheName);
    }

    private void closeCache(PersistentCacheManager localCache) {
        if (localCache != null && localCache.getStatus().toString().equals("AVAILABLE")) {
            localCache.close();
            localCache = null;
        }
    }

    /**
     * handle general boxes
     *
     * @param localField
     * @param localBoxValue
     * @throws IOException
     * @throws UnauthorizedAccessAttemptException
     * @throws InterruptedException
     */
    @SuppressWarnings({"rawtypes"})
    private void handleBox(String localField, String localBoxValue)
            throws IOException, UnauthorizedAccessAttemptException, InterruptedException {
        fillLists();
        searchResultNumber = tmpResultObjectIDs.size();
        String text;
        String text2;

        if (language.equals("Deutsch")) {
            text = "Resultate: " + searchResultNumber;
        } else {
            text = "Results: " + searchResultNumber;
        }

        if (localBoxValue != null) {
            text2 =
                    "Results for \""
                            + localBoxValue
                            + "\" in \""
                            + localField
                            + "\": "
                            + searchResultNumber;
            log(text2);
        }

        statusLabel = new Label(text, ContentMode.HTML);
        statusLayout.removeAllComponents();
        statusLayout.addComponent(statusLabel);
        statusLayout.setComponentAlignment(statusLabel, Alignment.MIDDLE_CENTER);
        paginationPanel.setAllEntries(tmpResultObjectIDs);

        // open required caches
        openValueCache();
        openRefineCache();

        sublocationHashMap = new HashMap<String, ArrayList>();
        fillHashMap(sublocationHashMap, tmpResultObjectIDs, "Iptc4xmpExt:Sublocation");
        if (language.equals("Deutsch")) {
            setBoxItems(sublocationBox, "Gebäude", sublocationHashMap);
        } else {
            setBoxItems(sublocationBox, "Building", sublocationHashMap);
        }

        cityHashMap = new HashMap<String, ArrayList>();
        fillHashMap(cityHashMap, tmpResultObjectIDs, "Iptc4xmpExt:City");
        if (language.equals("Deutsch")) {
            setBoxItems(cityBox, "Stadt", cityHashMap);
        } else {
            setBoxItems(cityBox, "City", cityHashMap);
        }

        stateHashMap = new HashMap<String, ArrayList>();
        fillHashMap(stateHashMap, tmpResultObjectIDs, "Iptc4xmpExt:ProvinceState");
        if (language.equals("Deutsch")) {
            setBoxItems(stateBox, "Bundesland", stateHashMap);
        } else {
            setBoxItems(stateBox, "State", stateHashMap);
        }
        // deactivate Date Refinement
        //    	dateList = new ArrayList();
        //    	fillDateList(dateList, tmpResultObjectIDs, min, max);

        // close required caches
        closeCache(refineCacheManager);
        closeCache(valueCacheManager);
    }

    // deactivate Date Refinement
    //    /**
    //     * handle date boxes
    //     *
    //     * @param localField
    //     * @param localBoxValue
    //     * @throws IOException
    //     * @throws UnauthorizedAccessAttemptException
    //     * @throws InterruptedException
    //     */
    //    @SuppressWarnings({ "rawtypes", "unchecked" })
    //    private void handleBoxDate(String localField, String localBoxValue)
    //			throws IOException, UnauthorizedAccessAttemptException, InterruptedException {
    //    	fillLists();
    //    	searchResultNumber = tmpResultObjectIDs.size();
    //    	String text = "";
    //    	String text2 = "";
    //
    //    	if ( language.equals("Deutsch") )
    //    	{
    //        	text = "Resultate: " + searchResultNumber;
    //    	}
    //    	else
    //    	{
    //        	text = "Results: " + searchResultNumber;
    //    	}
    //
    //    	if ( localBoxValue != null )
    //    	{
    //        	text2 = "Results for refinement \"" + localBoxValue + "\" in \"" + localField + "\":
    // " + searchResultNumber;
    //    	} else
    //    	{
    //        	text2 = "Results when refinement \"" + localBoxValue + "\" in \"" + localField + "\"
    // is removed: " + searchResultNumber;
    //    	}
    //
    //    	statusLabel = new Label(text, ContentMode.HTML);
    //        log(text2);
    //    	statusLayout.removeAllComponents();
    //    	statusLayout.addComponent(statusLabel);
    //    	statusLayout.setComponentAlignment(statusLabel, Alignment.MIDDLE_CENTER);
    //    	paginationPanel.setAllEntries(tmpResultObjectIDs);
    //
    //    	openValueCache();
    //      	openRefineCache();
    //
    //    	sublocationHashMap = new HashMap<String, ArrayList>();
    //    	fillHashMap(sublocationHashMap, tmpResultObjectIDs, "Iptc4xmpExt:Sublocation");
    //    	if ( language.equals("Deutsch") )
    //    	{
    //        	setBoxItems(sublocationBox, "Gebäude", sublocationHashMap);
    //    	}
    //    	else
    //    	{
    //        	setBoxItems(sublocationBox, "Building", sublocationHashMap);
    //    	}
    //
    //    	cityHashMap = new HashMap<String, ArrayList>();
    //    	fillHashMap(cityHashMap, tmpResultObjectIDs, "Iptc4xmpExt:City");
    //    	if ( language.equals("Deutsch") )
    //    	{
    //        	setBoxItems(cityBox, "Stadt", cityHashMap);
    //    	}
    //    	else
    //    	{
    //        	setBoxItems(cityBox, "City", cityHashMap);
    //    	}
    //
    //    	stateHashMap = new HashMap<String, ArrayList>();
    //    	fillHashMap(stateHashMap, tmpResultObjectIDs, "Iptc4xmpExt:ProvinceState");
    //    	if ( language.equals("Deutsch") )
    //    	{
    //          	setBoxItems(stateBox, "Bundesland", stateHashMap);
    //    	}
    //    	else
    //    	{
    //          	setBoxItems(stateBox, "State", stateHashMap);
    //    	}
    //    	ArrayList<String> dateList = new ArrayList();
    //    	fillDateList(dateList, tmpResultObjectIDs, minDate, maxDate);
    ////      	lowerDateField.setValue(min);
    ////      	upperDateField.setValue(max);
    //
    //    	closeCache(refineCacheManager);
    //    	closeCache(valueCacheManager);
    //
    //    }

    //    @SuppressWarnings({"unchecked", "rawtypes"})
    //    private void fillAndFindAndSetMinMax(
    //            ArrayList localDateList,
    //            List<String> localResultObjectIDs,
    //            String localRangeStart,
    //            String localRangeEnd)
    //            throws UnauthorizedAccessAttemptException, IOException {
    //        int count = 0;
    //        for (int i = 0; i < count; i++) {
    //            objectIdLocal = localResultObjectIDs.get(i);
    //            if (!valueCache.containsKey(objectIdLocal)) {
    //                fillObjectValuesMap();
    //                valueCache.put(objectIdLocal, valuesMapLocal);
    //            } else {
    //                String starttmp =
    //                        (String)
    // valueCache.get(objectIdLocal).get("cvma:AgeDeterminationStart");
    //                String endtmp =
    //                        (String)
    // valueCache.get(objectIdLocal).get("cvma:AgeDeterminationEnd");
    //                if (starttmp != null && endtmp != null) {
    //                    if ((localRangeStart.compareTo(starttmp) < 0
    //                                    || localRangeStart.compareTo(starttmp) == 0)
    //                            && (localRangeEnd.compareTo(endtmp) > 0
    //                                    || localRangeEnd.compareTo(endtmp) == 0)) {
    //                        localDateList.add(objectIdLocal);
    //                    }
    //
    //                    if (starttmp.compareTo(minDate) < 0) {
    //                        minDate = starttmp.substring(0, 4);
    //                        minDate = new Integer(minDate).toString();
    //                    }
    //
    //                    if (endtmp.compareTo(maxDate) > 0) {
    //                        maxDate = endtmp.substring(0, 4);
    //                        maxDate = new Integer(maxDate).toString();
    //                    }
    //                }
    //            }
    //        }
    //    }

    // fill tmpResultObjectIDs as new overall list of search results
    // depending on what criteria (state, city, sublocation) was chosen
    @SuppressWarnings("unchecked")
    private void fillLists() {
        boolean tmpused = false;
        tmpResultObjectIDs.clear();

        if (!subLocationResultObjectIDs.isEmpty()) {
            if (!tmpused) {
                tmpResultObjectIDs.addAll(subLocationResultObjectIDs);
                tmpused = true;
            } else {
                tmpResultObjectIDs =
                        (List<String>)
                                CollectionUtils.intersection(
                                        tmpResultObjectIDs, subLocationResultObjectIDs);
            }
        }

        if (!cityResultObjectIDs.isEmpty()) {
            if (tmpResultObjectIDs.isEmpty() && !tmpused) {
                tmpResultObjectIDs.addAll(cityResultObjectIDs);
                tmpused = true;
            } else {
                tmpResultObjectIDs =
                        (List<String>)
                                CollectionUtils.intersection(
                                        tmpResultObjectIDs, cityResultObjectIDs);
            }
        }

        if (!stateResultObjectIDs.isEmpty()) {
            if (tmpResultObjectIDs.isEmpty() && !tmpused) {
                tmpResultObjectIDs.addAll(stateResultObjectIDs);
                tmpused = true;
            } else {
                tmpResultObjectIDs =
                        (List<String>)
                                CollectionUtils.intersection(
                                        tmpResultObjectIDs, stateResultObjectIDs);
            }
        }

        if (!dateResultObjectIDs.isEmpty()) {
            if (tmpResultObjectIDs.isEmpty() && !tmpused) {
                tmpResultObjectIDs.addAll(dateResultObjectIDs);
                tmpused = true;
            } else {
                tmpResultObjectIDs =
                        (List<String>)
                                CollectionUtils.intersection(
                                        tmpResultObjectIDs, dateResultObjectIDs);
            }
        }

        if (sublocationBoxValue == null || cityBoxValue == null || stateBoxValue == null) {
            if (cityResultObjectIDs.isEmpty()
                    && stateResultObjectIDs.isEmpty()
                    && subLocationResultObjectIDs.isEmpty()
                    && dateResultObjectIDs.isEmpty()) {
                tmpResultObjectIDs.addAll(resultObjectIDs);
            }
        }
    }

    /**
     * set content and caption of combo box
     *
     * @param localBox
     * @param caption
     * @param localHashMap
     */
    @SuppressWarnings("rawtypes")
    private void setBoxItems(
            ComboBox<String> localBox, String caption, HashMap<String, ArrayList> localHashMap) {
        ArrayList<String> dynItemList = new ArrayList<String>();
        for (Map.Entry<String, ArrayList> stringArrayListEntry : localHashMap.entrySet()) {
            dynItemList.add((String) ((Map.Entry) stringArrayListEntry).getKey());
        }
        Collections.sort(dynItemList);
        localBox.setItems(dynItemList);
        localBox.setCaption(caption + " (" + dynItemList.size() + ")");
        localBox.setPlaceholder(" ");
    }

    /**
     * wait for cache lock to be freed by some other actor with sleep for avg. of 50ms do that 100
     * times then assume error and delete lock
     *
     * @param path
     * @throws InterruptedException
     */
    private void waitForFreeFileLock(String path) throws InterruptedException {
        File lock = new File(path + "/.lock");
        int counter = 0;
        while (lock.exists()) {
            Random ran = new Random();
            int sleepTime = ran.nextInt(75) + 25;
            TimeUnit.MILLISECONDS.sleep(sleepTime);
            counter = counter + 1;
            if (counter >= 100) {
                lock.delete();
                log(path + "/.lock" + " deleted, as it probably was left there due to an error");
            }
        }
        if (counter > 0 && counter < 100) {
            log("Waited " + counter + " times for " + path + "/.lock" + " to become free");
        }
    }

    /**
     * probably due to ehcache, keep last modified values of index and data files in sync, otherwise
     * the problem indicated by this log message "The index for data file ehcache-disk-store.data is
     * more recent than the data file itself" would get out of hand and the cache would regularly be
     * reseted
     */
    private void fixCacheTimestamps(String localPath, String localCacheName) {
        String fullPath = "";
        File dir = new File(localPath + "/file/");
        for (File file : dir.listFiles()) {
            if (file.getName().startsWith(localCacheName + "_") && file.isDirectory()) {
                fullPath = localPath + "/file/" + file.getName() + "/offheap-disk-store/";
            }
        }
        File indexFile = new File(fullPath, "ehcache-disk-store" + ".index");
        File dataFile = new File(fullPath, "ehcache-disk-store" + ".data");
        dataFile.setLastModified(indexFile.lastModified() + 1);
    }

    /** close all Vaadin sub windows */
    private void closeSubWindows() {
        ArrayList<Object> windowsList =
                new ArrayList<>(Arrays.asList(getUI().getWindows().toArray()));
        ArrayList<Object> windowsListToRemove = new ArrayList<Object>();
        for (Object window : windowsList) {
            getUI().removeWindow((Window) window);
        }
    }

    /**
     * based on logString give standard log message
     *
     * @param logString
     */
    public void log(String logString) {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss,SSS");
        System.out.println(
                dateFormat.format(date)
                        + " - MASIPORTLET - "
                        + user.getEmailAddress()
                        + " - "
                        + logString);
    }

    /**
     * get currently set Liferay language
     *
     * @return language string
     */
    private String getLanguage() {
        if (localRequest.getLocale().toString().equals("de_DE")) {
            return "Deutsch";
        } else {
            return "English";
        }
    }

    /**
     * fill object values map
     *
     * @throws UnauthorizedAccessAttemptException
     * @throws IOException
     */
    private void fillObjectValuesMap() throws UnauthorizedAccessAttemptException, IOException {
        String key;
        String value;

        // Map that the key value pairs extract form mets are stored in
        valuesMapLocal = new HashMap<String, String>();
        if (mdmDo == null) {
            mdmDo =
                    MetaDataManagement.getMetaDataManagement()
                            .getMetaDataManager("DataOrganizationPU");
            mdmDo.setAuthorizationContext(getAuthorizationContext());
        }

        FileNode fileNode =
                mdmDo.findSingleResult(
                        "Select t FROM DataOrganizationNode t WHERE t.name=?1",
                        new Object[] {"mets_" + objectIdLocal + ".xml"},
                        FileNode.class);

        //	mdmDo.close();
        String path = fileNode.getLogicalFileName().getStringRepresentation().substring(5);
        String xmlDocument = new String(Files.readAllBytes(Paths.get(path)));
        String metsString = XML.toJSONObject(xmlDocument).toString();

        // add path to mets file
        valuesMapLocal.put("metspath", path);

        // create new json parser
        JsonParser jsonparser = new JsonParser();

        // create json object with metsString as input
        JsonObject jsonObject = jsonparser.parse(metsString).getAsJsonObject();

        // digital object ID
        key = "digitalObjectID";
        value = jsonObject.getAsJsonObject("mets").getAsJsonPrimitive("OBJID").getAsString();
        valuesMapLocal.put(key, value);

        // createDate
        valuesMapLocal.put(
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
                valuesMapLocal.put(
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
                valuesMapLocal.put(
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

                            List<String> imageextensions = new ArrayList<String>();
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
                                valuesMapLocal.put(key, value);
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
                            valuesMapLocal.put(key, value);
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

            // only store important metadata
            value = null;
            String[] keys = {
                "Iptc4xmpExt:Sublocation",
                "xmprights:UsageTerms",
                "cvma:Type",
                //   	            		 "xmlns:Iptc4xmpExt",
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
                value = extractdmdSecMD("cvma:cvma", keyEntry, jsonObject, dmdSections.get("CVMA"));
                if (value != null) {
                    valuesMapLocal.put(keyEntry, value);
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
                        extractdmdSecMD(
                                "oai_dc:dc", keyEntry, jsonObject, dmdSections.get("DUBLIN-CORE"));
                valuesMapLocal.put(keyEntry, value);
            }
        }

        String date = valuesMapLocal.get("createDate");
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
                        + valuesMapLocal.get("distinguishedName")
                        + "/ingest_"
                        + valuesMapLocal.get("baseId")
                        + "_"
                        + objectIdLocal
                        + "/";
        // store path in map of digital object
        valuesMapLocal.put("localpath", localPath);

        // osm link
        if (valuesMapLocal.containsKey("cvma:GPSLatitude")
                && valuesMapLocal.containsKey("cvma:GPSLongitude")) {
            if (valuesMapLocal.get("cvma:GPSLatitude") != null
                    && valuesMapLocal.get("cvma:GPSLongitude") != null) {
                valuesMapLocal.put(
                        "osmlink",
                        "https://www.openstreetmap.org/?mlat="
                                + valuesMapLocal.get("cvma:GPSLatitude")
                                + "&mlon="
                                + valuesMapLocal.get("cvma:GPSLongitude")
                                + "#map=18/"
                                + valuesMapLocal.get("cvma:GPSLatitude")
                                + "/"
                                + valuesMapLocal.get("cvma:GPSLongitude"));
            }
        }
    }

    private String extractdmdSecMD(String section, String key, JsonObject jsonObject, int counter) {
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
