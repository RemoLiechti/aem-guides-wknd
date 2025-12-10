/*
 *  Copyright 2019 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentContext;
import com.adobe.aem.guides.wknd.core.models.Byline;
import com.adobe.cq.wcm.core.components.models.Image;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {Byline.class},
        resourceType = {BylineImpl.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class BylineImpl implements Byline {
    protected static final String RESOURCE_TYPE = "wknd/components/byline";

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private ModelFactory modelFactory;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    protected ComponentContext componentContext;

    @ValueMapValue
    private String name;

    @ValueMapValue
    private List<String> occupations;

    private Image image;

    // Add a logger for any errors
    private static final Logger LOGGER = LoggerFactory.getLogger(BylineImpl.class);
    
    // Locks for deadlock demonstration
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    @PostConstruct
    private void init() {
        image = modelFactory.getModelFromWrappedRequest(request, request.getResource(), Image.class);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<String> getOccupations() {
        // DEMO: Intentional deadlock for demonstration purposes
        LOGGER.warn("DEMO: Starting deadlock demonstration in getOccupations()");
        
        // Create Thread 1: acquires LOCK_A, then tries to acquire LOCK_B
        Thread thread1 = new Thread(() -> {
            LOGGER.warn("DEMO Thread 1: Attempting to acquire LOCK_A");
            synchronized (LOCK_A) {
                LOGGER.warn("DEMO Thread 1: Acquired LOCK_A, sleeping for 100ms");
                try {
                    Thread.sleep(100); // Give thread2 time to acquire LOCK_B
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.warn("DEMO Thread 1: Now attempting to acquire LOCK_B");
                synchronized (LOCK_B) {
                    LOGGER.warn("DEMO Thread 1: Acquired LOCK_B (this won't happen - deadlock!)");
                }
            }
        }, "SomeDemoThread-1");
        
        // Create Thread 2: acquires LOCK_B, then tries to acquire LOCK_A
        Thread thread2 = new Thread(() -> {
            LOGGER.warn("DEMO Thread 2: Attempting to acquire LOCK_B");
            synchronized (LOCK_B) {
                LOGGER.warn("DEMO Thread 2: Acquired LOCK_B, sleeping for 100ms");
                try {
                    Thread.sleep(100); // Give thread1 time to acquire LOCK_A
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.warn("DEMO Thread 2: Now attempting to acquire LOCK_A");
                synchronized (LOCK_A) {
                    LOGGER.warn("DEMO Thread 2: Acquired LOCK_A (this won't happen - deadlock!)");
                }
            }
        }, "SomeDemoThread-2");
        
        // Start both threads
        thread1.start();
        thread2.start();
        
        // Wait for threads to complete (they won't - deadlock!)
        try {
            LOGGER.warn("DEMO Main: Waiting for threads to complete (will timeout after 2 seconds)");
            thread1.join(2000); // Wait max 2 seconds
            thread2.join(2000);
            
            if (thread1.isAlive() || thread2.isAlive()) {
                LOGGER.error("DEMO: DEADLOCK DETECTED! Threads are still alive after timeout");
                LOGGER.error("DEMO Thread 1 state: " + thread1.getState());
                LOGGER.error("DEMO Thread 2 state: " + thread2.getState());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("DEMO: Main thread interrupted", e);
        }
        
        if (occupations != null) {
            Collections.sort(occupations);
            return new ArrayList<String>(occupations);
        } else {
            return Collections.emptyList();
        }
   }

    @Override
    public boolean isEmpty() {
        final Image image = getImage();

        if (StringUtils.isBlank(name)) {
            // Name is missing, but required
            return true;
        } else if (occupations == null || occupations.isEmpty()) {
            // At least one occupation is required
            return true;
        } else if (image == null || StringUtils.isBlank(image.getSrc())) {
            // A valid image is required
            return true;
        } else {
            // Everything is populated, so this component is not considered empty
            return false;
        }
    }

    /**
     * @return the Image Sling Model of this resource, or null if the resource cannot create a valid Image Sling Model. 
     */
    private Image getImage() {
        return image;
    }
}