/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.ui.internal.util;

import com.google.dart.compiler.util.apache.ObjectUtils;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.ProjectManager;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;

import java.util.List;

/**
 * Utility methods for launching that use the engine model to get information
 */
public class NewLaunchUtils {

  /**
   * Return the best launch configuration to run for the given resource.
   * 
   * @param resource
   * @return
   * @throws DartModelException
   */
  public static ILaunchConfiguration getLaunchFor(IResource resource) throws DartModelException {
    // If it's a project, find any launches in that project.
    if (resource instanceof IProject) {
      IProject project = (IProject) resource;

      List<ILaunchConfiguration> launches = LaunchUtils.getLaunchesFor(project);

      if (launches.size() > 0) {
        return LaunchUtils.chooseLatest(launches);
      }
    }

    List<ILaunchConfiguration> configs = LaunchUtils.getExistingLaunchesFor(resource);

    if (configs.size() > 0) {
      return LaunchUtils.chooseLatest(configs);
    }

    // No existing configs - check if the current resource is not launchable.
//    if (getApplicableLaunchShortcuts(resource).size() == 0) {
//      // Try and locate a launchable library that references this library.
//    LibraryElement[] libraries = getLibraries(resource);
//
//      if (libraries.length > 0) {
//        Set<ILaunchConfiguration> libraryConfigs = new HashSet<ILaunchConfiguration>();
//
//        for (LibraryElement library : libraries) {
//          for (DartLibrary referencingLib : library.getReferencingLibraries()) {
//            IResource libResource = referencingLib.getCorrespondingResource();
//
//            libraryConfigs.addAll(getExistingLaunchesFor(libResource));
//          }
//        }
//
//        if (libraryConfigs.size() > 0) {
//          return chooseLatest(libraryConfigs);
//        }
//      }
//    }

    return null;
  }

  /**
   * Returns the LibraryElement for the dart library associated with the file, if any
   * 
   * @return LibraryElement or <code>null</code>
   */
  public static LibraryElement[] getLibraries(IResource resource) {
    // TODO(keertip): replace this with a call to method that gets Source about libraries with an entrypoint 
    // and can be launched in the browser. The resolved LibraryElement is not necessary for this task. 
    // Wait for API  to be implemented in AnalysisContext
    ProjectManager manager = DartCore.getProjectManager();
    if (resource instanceof IFile) {
      if (DartCore.isDartLikeFileName(resource.getName())) {
        LibraryElement element = manager.getLibraryElement((IFile) resource);
        if (element != null) {
          return new LibraryElement[] {element};
        }
        if (DartCore.isHTMLLikeFileName(resource.getName())) {
          HtmlElement htmlElement = manager.getHtmlElement((IFile) resource);
          if (htmlElement != null) {
            return htmlElement.getLibraries();
          }
        }
      } else {
        resource = resource.getParent();
      }
    }
    if (resource instanceof IContainer) {
      return manager.getLibraries((IContainer) resource);
    }
    return new LibraryElement[] {};
  }

  /**
   * @param resource
   * @param config
   * @return whether the given launch config could be used to launch the given resource
   */
  public static boolean isLaunchableWith(IResource resource, ILaunchConfiguration config) {
    DartLaunchConfigWrapper launchWrapper = new DartLaunchConfigWrapper(config);

    IResource appResource = launchWrapper.getApplicationResource();

    if (ObjectUtils.equals(appResource, resource)) {
      return true;
    }

    // TODO: this does not use the launch configurations correctly

//    DartLibrary[] testLibraries = LaunchUtils.getDartLibraries(resource);
//    DartLibrary[] existingLibrary = LaunchUtils.getDartLibraries(launchWrapper.getApplicationResource());
//
//    if (testLibraries.length > 0 & existingLibrary.length > 0) {
//      for (DartLibrary testLibrary : testLibraries) {
//        if (testLibrary.equals(existingLibrary[0])) {
//          return true;
//        }
//      }
//    }

    return false;
  }

}