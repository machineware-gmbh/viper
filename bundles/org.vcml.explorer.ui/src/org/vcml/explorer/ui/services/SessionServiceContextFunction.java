/******************************************************************************
 *                                                                            *
 * Copyright 2018 Jan Henrik Weinstock                                        *
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

import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.osgi.service.component.annotations.Component;

@Component(name = "SessionServiceContextFunction", service = IContextFunction.class, property = "service.context.key=org.vcml.explorer.ui.services.ISessionService")
public class SessionServiceContextFunction extends ContextFunction {
    @Override
    public Object compute(IEclipseContext context, String contextKey) {
        System.out.println("session service context function");
        ISessionService sessionService = ContextInjectionFactory.make(SessionService.class, context);

        MApplication app = context.get(MApplication.class);
        IEclipseContext appCtx = app.getContext();
        appCtx.set(ISessionService.class, sessionService);

        return sessionService;
    }
}
