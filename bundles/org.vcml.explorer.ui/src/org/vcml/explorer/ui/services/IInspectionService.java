/******************************************************************************
 *                                                                            *
 * Copyright 2022 MachineWare GmbH                                            *
 * All Rights Reserved                                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 *                                                                            *
 ******************************************************************************/

package org.vcml.explorer.ui.services;

import org.vcml.session.Module;

public interface IInspectionService {

    /**
     * Register a module kind to be inspected by the provided view part
     * @param kind  Kind of module to inspect (processor, bus, memory...)
     * @param part  Class name of part that provides the UI
     */
    public void registerModulePart(String kind, String part);

    /**
     * Returns whether any UI part is available to inspect the specified module
     * @param module    Module to inspect
     * @return          <tt>true</tt> if <tt>module</tt> can be inspected
     */
    public boolean isInspectable(Module module);

    /**
     * Returns the URI of the part that handles the inspection of the specified module.
     * @param module    Module to inspect
     * @return          URI of the UI part that handles inspection or <tt>null</tt>
     */
    public String lookupPartContributionURI(Module module);

}
