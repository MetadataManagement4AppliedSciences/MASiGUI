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
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import com.vaadin.event.LayoutEvents;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import edu.kit.dama.authorization.entities.IAuthorizationContext;
import edu.kit.dama.authorization.exceptions.UnauthorizedAccessAttemptException;

/**
 * Pagination panel component used to render and navigate through search results.
 *
 * @author Richard Grunzke
 */
public class PaginationPanel extends CustomComponent {
    private static final long serialVersionUID = 1L;

    private VerticalLayout mainLayout;
    private VerticalLayout pageLayout;
    private HorizontalLayout navigation;
    private int currentPage = 0;
    private int overallPages = 0;
    private final MyPortletUI parent;
    private final List<String> allEntries = new LinkedList<>();
    private List<String> objectsOnPage = null;
    private PersistentCacheManager cacheManager = null;
    Cache<String, HashMap> valueCache;
    private VerticalLayout localRefineLayout;
    private String cacheName = null;

    /**
     * Default constructor.
     *
     * @param pParent The parent used to obtain the authorization context.
     */
    public PaginationPanel(MyPortletUI pParent) {
        parent = pParent;
        buildMainLayout();
        setCompositionRoot(mainLayout);
    }

    /**
     * Set the list of all elements (digital Object IDs) which can be rendered.
     *
     * @param pObjects All digital object ids.
     * @throws IOException
     * @throws InterruptedException
     */
    public final void setAllEntries(List<String> pObjects)
            throws IOException, InterruptedException {
        allEntries.clear();
        allEntries.addAll(pObjects);
        overallPages = allEntries.size() / parent.entriesPerPage;
        overallPages += (allEntries.size() % parent.entriesPerPage > 0) ? 1 : 0;
        currentPage = 0;
        updatePage();
    }

    /**
     * Get the parent UI.
     *
     * @return The parent UI.
     */
    public final MyPortletUI getParentUI() {
        return parent;
    }

    @Override
    protected final void setCompositionRoot(Component compositionRoot) {
        super.setCompositionRoot(compositionRoot);
    }

    /** Build the main layout. */
    private void buildMainLayout() {
        mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setStyleName("white");
        mainLayout.setMargin(new MarginInfo(false, false, false, false));

        if (localRefineLayout != null) {
            mainLayout.addComponent(localRefineLayout);
            mainLayout.setComponentAlignment(localRefineLayout, Alignment.MIDDLE_CENTER);
        }

        navigation = new HorizontalLayout();
        navigation.setSizeFull();
        navigation.setStyleName("white");
        navigation.setMargin(new MarginInfo(false, false, false, false));

        mainLayout.addComponent(navigation);
        mainLayout.setComponentAlignment(navigation, Alignment.MIDDLE_CENTER);

        pageLayout = new VerticalLayout();
        pageLayout.setSizeFull();
        pageLayout.setStyleName("white");
        pageLayout.setMargin(new MarginInfo(false, false, false, false));

        mainLayout.addComponent(pageLayout);
        mainLayout.setComponentAlignment(pageLayout, Alignment.MIDDLE_CENTER);
    }

    /**
     * Update the currently rendered page.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void updatePage() throws IOException, InterruptedException {

        // start with empty layout, avoids adding search results to existing ones when a new search
        // query is executed
        pageLayout.removeAllComponents();

        // sublist of the digital object IDs that are to be display on one page
        objectsOnPage =
                allEntries.subList(
                        currentPage * parent.entriesPerPage,
                        Math.min(
                                currentPage * parent.entriesPerPage + parent.entriesPerPage,
                                allEntries.size()));

        int cnt = 0;
        IAuthorizationContext ctx = parent.getAuthorizationContext();

        if (objectsOnPage.size() > cnt) {
            openValueCache();
            valueCache = cacheManager.getCache(cacheName, String.class, HashMap.class);
        }

        while (cnt < parent.entriesPerPage) {
            if (objectsOnPage.size() > cnt) {
                String entryId;
                try {
                    entryId = objectsOnPage.get(cnt).trim();
                    pageLayout.addComponent(new EntryRenderPanel(this, ctx, entryId));
                } catch (UnauthorizedAccessAttemptException ex) {
                    // do nothing, entry stays null
                }
            }
            cnt++;
        }

        closeValueCache();

        // update navigation
        if (!objectsOnPage.isEmpty()) {
            HorizontalLayout newNavigation = buildNavigationComponent();
            mainLayout.replaceComponent(navigation, newNavigation);
            navigation = newNavigation;
        } else {
            HorizontalLayout newNavigation = new HorizontalLayout();
            mainLayout.replaceComponent(navigation, newNavigation);
            navigation = newNavigation;
        }
    }

    /**
     * Build the navigation layout including the appropriate buttons to navigate through the
     * pagination pages.
     *
     * @return The navigation layout component.
     */
    private HorizontalLayout buildNavigationComponent() {

        HorizontalLayout resultNavigation = new HorizontalLayout();

        // add "JumpToFirstPage" button
        final Button first = new Button();
        if (parent.language.equals("Deutsch")) {
            first.setCaption("Erste Seite");
        } else {
            first.setCaption("First Page");
        }
        first.addStyleName(ValoTheme.BUTTON_SMALL);
        first.addStyleName(ValoTheme.LABEL_SMALL);
        first.addClickListener(
                new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        parent.log("Click on first button");
                        currentPage = 0;
                        try {
                            updatePage();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

        // add "PreviousPage" button
        final Button prev = new Button();
        if (parent.language.equals("Deutsch")) {
            prev.setCaption("Vorherige Seite");
        } else {
            prev.setCaption("Previous Page");
        }
        prev.addStyleName(ValoTheme.BUTTON_SMALL);
        prev.addStyleName(ValoTheme.LABEL_SMALL);
        prev.addClickListener(
                new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        parent.log("Click on prev button");
                        if (currentPage > 0) {
                            currentPage--;
                            try {
                                updatePage();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        // add "NextPage" button
        final Button next = new Button();
        if (parent.language.equals("Deutsch")) {
            next.setCaption("Nächste Seite");
        } else {
            next.setCaption("Next Page");
        }
        next.addStyleName(ValoTheme.BUTTON_SMALL);
        next.addStyleName(ValoTheme.LABEL_SMALL);
        next.addClickListener(
                new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        parent.log("Click on next button");
                        if (currentPage + 1 < overallPages) {
                            currentPage++;
                            try {
                                updatePage();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        // add "JumpToLastPage" button
        final Button last = new Button();
        if (parent.language.equals("Deutsch")) {
            last.setCaption("Letzte Seite");
        } else {
            last.setCaption("Last Page");
        }
        last.addStyleName(ValoTheme.BUTTON_SMALL);
        last.addStyleName(ValoTheme.LABEL_SMALL);
        last.addClickListener(
                new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void buttonClick(Button.ClickEvent event) {
                        parent.log("Click on last button");
                        currentPage = overallPages - 1;
                        try {
                            updatePage();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

        // enable/disable buttons depending on the current page
        if (overallPages == 0) {
            first.setEnabled(false);
            prev.setEnabled(false);
            next.setEnabled(false);
            last.setEnabled(false);
        } else {
            first.setEnabled(!(currentPage == 0) || !(overallPages < 2));
            prev.setEnabled(!(currentPage == 0) || !(overallPages < 2));
            next.setEnabled(!(currentPage == overallPages - 1) || !(overallPages < 2));
            last.setEnabled(!(currentPage == overallPages - 1) || !(overallPages < 2));
        }

        Label leftFiller = new Label();
        resultNavigation.addComponent(leftFiller);
        resultNavigation.setExpandRatio(leftFiller, 1.0f);
        resultNavigation.addComponent(first);
        resultNavigation.addComponent(prev);
        int start = currentPage - 5;
        start = (start < 0) ? 0 : start;
        int end = start + 10;
        end = (end > overallPages) ? overallPages : end;

        if (end - start < 10 && overallPages > 10) {
            start = end - 10;
        }

        if (overallPages == 0) {
            Label noEntryLabel = new Label("                 ", ContentMode.HTML);
            noEntryLabel.setSizeUndefined();
            resultNavigation.addComponent(noEntryLabel);
            resultNavigation.setComponentAlignment(noEntryLabel, Alignment.MIDDLE_CENTER);
        }

        // build the actual page entries
        for (int i = start; i < end; i++) {
            if (i == currentPage) {
                // the current page is marked with a special style
                Label pageLink = new Label("<b>" + (i + 1) + "</b>", ContentMode.HTML);
                pageLink.setEnabled(false);
                resultNavigation.addComponent(pageLink);
                resultNavigation.setComponentAlignment(pageLink, Alignment.MIDDLE_CENTER);
            } else {
                // otherwise normal links are added, click-events are handled via
                // LayoutClickListener
                Link pageLink = new Link(Integer.toString(i + 1), null);
                pageLink.isEnabled();
                resultNavigation.addComponent(pageLink);
                resultNavigation.setComponentAlignment(pageLink, Alignment.MIDDLE_CENTER);
            }
        }

        // add right navigation buttons
        resultNavigation.addComponent(next);
        resultNavigation.addComponent(last);

        // ...and fill the remaining space
        Label rightFiller = new Label();
        resultNavigation.addComponent(rightFiller);
        resultNavigation.setExpandRatio(rightFiller, 1.0f);

        // put everything in the middle
        resultNavigation.setComponentAlignment(first, Alignment.MIDDLE_CENTER);
        resultNavigation.setComponentAlignment(prev, Alignment.MIDDLE_CENTER);
        resultNavigation.setComponentAlignment(next, Alignment.MIDDLE_CENTER);
        resultNavigation.setComponentAlignment(last, Alignment.MIDDLE_CENTER);

        // add layout click listener to be able to navigate by clicking the single pages
        resultNavigation.addLayoutClickListener(
                new LayoutEvents.LayoutClickListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void layoutClick(LayoutEvents.LayoutClickEvent event) {
                        Component child = event.getChildComponent();
                        if (child != null && child instanceof Link) {
                            currentPage = Integer.parseInt(child.getCaption()) - 1;
                            try {
                                updatePage();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        // finalize
        resultNavigation.setSizeFull();
        resultNavigation.setMargin(false);
        resultNavigation.setHeight("50px");
        resultNavigation.setStyleName("white");
        return resultNavigation;
    }

    private void openValueCache() throws InterruptedException {
        closeValueCache();
        cacheName = "valueCache";
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
                cacheManager =
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
                e1.printStackTrace();
            }
        } while (OverlappingFileLockExceptionThrown);
        if (counter > 0) {
            parent.log("creation of " + cacheName + " retriggered " + counter + " times");
        }
        valueCache = cacheManager.getCache(cacheName, String.class, HashMap.class);
        fixCacheTimestamps(storePath, cacheName);
    }

    private void closeValueCache() {
        if (cacheManager != null && cacheManager.getStatus().toString().equals("AVAILABLE")) {
            cacheManager.close();
            cacheManager = null;
        }
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
                parent.log(
                        path
                                + "/.lock"
                                + " deleted, as it probably was left there due to an error");
            }
        }
        if (counter > 0 && counter < 100) {
            parent.log("Waited " + counter + " times for " + path + "/.lock" + " to become free");
        }
    }

    /**
     * keep last modified values of index and data files in sync, otherwise the problem indicated by
     * this log message "The index for data file ehcache-disk-store.data is more recent than the
     * data file itself" would get out of hand and the cache would regularly be reseted
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
}
