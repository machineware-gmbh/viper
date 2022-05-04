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
package org.vcml.explorer.ui;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.vcml.session.Attribute;
import org.vcml.session.Module;
import org.vcml.session.Session;

public class Resources {

    public static Color getColor(int systemColorID) {
        Display display = Display.getCurrent();
        return display.getSystemColor(systemColorID);
    }

    private static Map<String, Image> images = new HashMap<String, Image>();

    public static Image getImage(String path) {
        Image image = images.get(path);
        if (image == null) {
            Bundle bundle = FrameworkUtil.getBundle(Resources.class);
            URL url = FileLocator.find(bundle, new Path(path), null);
            ImageDescriptor desc = ImageDescriptor.createFromURL(url);
            image = desc.createImage();
            images.put(path, image);
        }

        return image;
    }

    public static Image getImageFor(Object object) {
        if (object instanceof Session)
            return getImageForSession((Session) object);
        else if (object instanceof Module)
            return getImageForModule((Module) object);
        else if (object instanceof Attribute)
            return getImage("icons/attribute.gif");
        else
            return getImage("icons/null");
    }

    public static Image getImageForSession(Session session) {
        if (session.isConnected())
            return getImage("icons/session_alt.gif");
        else
            return getImage("icons/session.gif");
    }

    public static Image getImageForModule(Module module) {
        String kind = module.getKind();
        if (kind == null) {
            System.out.println("module has null kind " + module);
            return getImage("icons/chip.png");
        }
        switch (module.getKind()) {
        case Module.KIND_SC_OBJECT:
            return getImage("icons/chip.png");
        case Module.KIND_SC_MODULE:
            return getImage("icons/chip.png");
        case Module.KIND_SC_SIGNAL:
            return getImage("icons/signal.png");
        case Module.KIND_SC_PORT:
            return getImage("icons/out.png");
        case Module.KIND_SC_EXPORT:
            return getImage("icons/in.png");
        case Module.KIND_SC_OUT:
            return getImage("icons/out.png");
        case Module.KIND_SC_IN:
            return getImage("icons/in.png");
        case Module.KIND_SC_METHOD_PROCESS:
            return getImage("icons/method.gif");
        case Module.KIND_SC_THREAD_PROCESS:
            return getImage("icons/thread.gif");
        case Module.KIND_TLM_INITIATOR_SOCKET:
            return getImage("icons/initiator.png");
        case Module.KIND_TLM_TARGET_SOCKET:
            return getImage("icons/target.png");
        case Module.KIND_VCML_IN_PORT:
            return getImage("icons/in.png");
        case Module.KIND_VCML_OUT_PORT:
            return getImage("icons/out.png");
        case Module.KIND_VCML_MASTER_SOCKET:
            return getImage("icons/initiator.png");
        case Module.KIND_VCML_SLAVE_SOCKET:
            return getImage("icons/target.png");
        case Module.KIND_VCML_COMPONENT:
            return getImage("icons/chip.png");
        case Module.KIND_VCML_PROCESSOR:
            return getImage("icons/chip.png");
        case Module.KIND_VCML_BUS:
            return getImage("icons/bus.gif");
        case Module.KIND_VCML_MEMORY:
            return getImage("icons/chip.png");
        case Module.KIND_VCML_XBAR:
            return getImage("icons/chip.png");
        case Module.KIND_VCML_PERIPHERAL:
            return getImage("icons/chip.png");
        case Module.KIND_VCML_UART8250:
            return getImage("icons/chip.png");
        case Module.KIND_VCML_ETHERNET:
            return getImage("icons/chip.png");
        default:
            return getImage("icons/chip.png");
        }
    }

    public static void disposeImages() {
        for (Image image : images.values())
            image.dispose();
        images.clear();
    }

    public static Font getMonoSpaceFont() {
        return JFaceResources.getFont(JFaceResources.TEXT_FONT);
    }

    public static void dispose() {
        disposeImages();
        //disposeFonts();
        //disposeCursors();
    }
}
