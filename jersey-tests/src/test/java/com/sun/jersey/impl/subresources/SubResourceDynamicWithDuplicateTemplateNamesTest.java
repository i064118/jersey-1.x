/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.jersey.impl.subresources;

import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Map.Entry;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.impl.AbstractResourceTester;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class SubResourceDynamicWithDuplicateTemplateNamesTest extends AbstractResourceTester {
    
    public SubResourceDynamicWithDuplicateTemplateNamesTest(String testName) {
        super(testName);
    }
    
    @Path("/{v}")
    static public class Parent {
        @Path("child/")
        public Child getChild(@PathParam("v") String v) {
            return new Child(v);
        }
    }
    
    public static class Child {

        private StringBuilder buffer;
        
        public Child(String v) {
            this.buffer = new StringBuilder(v).append(" -> ");
        }

        @Override
        public String toString() {
            return this.buffer.toString();
        }

        @GET
        public String getMe(@PathParam("v") String v) {
            return this.buffer.append("me() : ").append(v).toString();
        }

        @GET
        @Path("next/{v}")
        public String getMeAndNext(@PathParam("v") String next) {
            return this.buffer.append("next() : ").append(next).toString();
        }

        @GET
        @Path("all")
        public String getAllParams(@Context UriInfo uriInfo) {
            final MultivaluedMap<String, String> params = uriInfo.getPathParameters();

            StringBuilder sb = new StringBuilder();
            for (Entry<String, List<String>> e : params.entrySet()) {
                sb.append("Param '").append(e.getKey()).append("' values:");
                for (String value : e.getValue()) {
                    sb.append(' ').append(value);
                }
            }

            return sb.toString();
        }

        @Path("{v}")
        public Child getChild(@PathParam("v") String v) {
            this.buffer.append(v).append(" -> ");

            return this;
        }
    }
    
    public void testSubResourceDynamicWithTemplates() {
        initiateWebApplication(Parent.class);

        // Parent.getChild(...) -> Child.getMe(...)
        assertEquals("parent -> me() : parent", resource("/parent/child").get(String.class));

        // Parent.getChild(...) -> Child.getChild(...) -> Child.getMe(...)
        assertEquals("parent -> first -> me() : first", resource("/parent/child/first").get(String.class));

        // Parent.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getMe(...)
        assertEquals("parent -> first -> second -> me() : second", resource("/parent/child/first/second").get(String.class));

        // Parent.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getMe(...)
        assertEquals("parent -> first -> second -> third -> me() : third", resource("/parent/child/first/second/third").get(String.class));

        // Parent.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getMeAndNext(...)
        assertEquals("parent -> first -> second -> third -> next() : fourth", resource("/parent/child/first/second/third/next/fourth").get(String.class));

        // Parent.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getChild(...) -> Child.getAllParams(...)
        assertEquals("Param 'v' values: fourth third second first parent", resource("/parent/child/first/second/third/fourth/all").get(String.class));
    }    
}
