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

import java.util.HashMap;
import java.util.Map;

import org.vcml.session.Module;


public class InspectionService implements IInspectionService {

    public static final String BUNDLE_URI = "bundleclass://org.vcml.explorer.ui";

    private Map<String, String> parts = new HashMap<String, String>();

    @Override
    public void registerModulePart(String kind, String part) {
        String uri = BUNDLE_URI + "/" + part;
        parts.put(kind, uri);
    }

    @Override
    public boolean isInspectable(Module module) {
        return parts.containsKey(module.getKind());
    }

    @Override
    public String lookupPartContributionURI(Module module) {
        return parts.get(module.getKind());
    }

    public InspectionService() { // find a better way to do this
        registerModulePart(Module.KIND_VCML_MEMORY, "org.vcml.explorer.ui.parts.MemoryPart");
        registerModulePart(Module.KIND_VCML_TERMINAL, "org.vcml.explorer.ui.parts.UartPart");
        registerModulePart(Module.KIND_VCML_PROCESSOR, "org.vcml.explorer.ui.parts.ProcessorPart");
        registerModulePart(Module.KIND_SIMV_HART, "org.vcml.explorer.ui.parts.ProcessorPart");
        registerModulePart(Module.KIND_SIMA_CORE, "org.vcml.explorer.ui.parts.ProcessorPart");
        registerModulePart(Module.KIND_USIMA_CORE, "org.vcml.explorer.ui.parts.ProcessorPart");
    }

}
