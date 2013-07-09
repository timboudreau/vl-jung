/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timboudreau.maven.dependency.graph.fork;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.openide.modules.Dependency;
import org.openide.modules.ModuleInfo;
import org.openide.modules.ModuleInstall;
import org.openide.modules.Modules;

public class Installer extends ModuleInstall {

    private static final String[] SIBLINGS = new String[]{
        "org.netbeans.modules.maven.indexer",
        "org.netbeans.modules.maven",
        "org.netbeans.modules.maven.embedder",
        "org.netbeans.modules.maven.model",
    };

    protected Set<String> friends() {
        return Collections.emptySet();
    }

    protected Set<String> siblings() {
        return new HashSet<>(Arrays.asList(SIBLINGS));
    }

    
    // Borrowed code from Jesse's Yenta so we don't need a direct dependency
    // (in which case users would not be able to install it)
    @Override
    public void validate() throws IllegalStateException {
        Set<String> friends = friends();
        Set<String> siblings = siblings();
        if (friends.isEmpty() && siblings.isEmpty()) {
            throw new IllegalStateException("Must specify some friends and/or siblings");
        }
        ModuleInfo me = Modules.getDefault().ownerOf(getClass());
        if (me == null) {
            throw new IllegalStateException("No apparent module owning " + getClass());
        }
        try {
            Object manager = me.getClass().getMethod("getManager").invoke(me);
            for (String m : friends) {
                if (siblings.contains(m)) {
                    throw new IllegalStateException("Cannot specify the same module " + m + " in both friends and siblings");
                }
                Object data = data(findDependency(manager, m));
                Field friendNamesF = Class.forName("org.netbeans.ModuleData", true, data.getClass().getClassLoader()).getDeclaredField("friendNames");
                friendNamesF.setAccessible(true);
                Set<?> names = (Set<?>) friendNamesF.get(data);
                Set<Object> newNames = new HashSet<Object>(names);
                newNames.add(me.getCodeNameBase());
                friendNamesF.set(data, newNames);
            }
            for (String m : siblings) {
                ModuleInfo dep = findDependency(manager, m);
                String implVersion = dep.getImplementationVersion();
                if (implVersion == null) {
                    throw new IllegalStateException("No implementation version found in " + m);
                }
                Object data = data(me);
                Field dependenciesF = Class.forName("org.netbeans.ModuleData", true, data.getClass().getClassLoader()).getDeclaredField("dependencies");
                dependenciesF.setAccessible(true);
                Dependency[] dependencies = (Dependency[]) dependenciesF.get(data);
                boolean found = false;
                for (int i = 0; i < dependencies.length; i++) {
                    if (dependencies[i].getName().replaceFirst("/.+$", "").equals(m)) {
                        Set<Dependency> nue = Dependency.create(Dependency.TYPE_MODULE, dependencies[i].getName() + " = " + implVersion);
                        if (nue.size() != 1) {
                            throw new IllegalStateException("Could not recreate dependency from " + dependencies[i] + " based on " + implVersion);
                        }
                        dependencies[i] = nue.iterator().next();
                        found = true;
                    }
                }
                if (!found) {
                    new IllegalStateException("Did not find dependency on " + m).printStackTrace();
                    continue;
                }
                // StandardModule.classLoaderUp skips adding a parent if the dep seemed to offer us nothing, and this has already been called.
                Object[] publicPackages = (Object[]) dep.getClass().getMethod("getPublicPackages").invoke(dep);
                if (publicPackages != null && publicPackages.length == 0) {
                    me.getClassLoader().getClass().getMethod("append", ClassLoader[].class).invoke(me.getClassLoader(), (Object) new ClassLoader[]{dep.getClassLoader()});
                }
            }
        } catch (IllegalStateException x) {
            throw x;
        } catch (Exception x) {
            throw new IllegalStateException(x);
        }
    }

    private ModuleInfo findDependency(/*ModuleManager*/Object manager, String m) throws Exception {
        Object dep = manager.getClass().getMethod("get", String.class).invoke(manager, m);
        if (dep == null) {
            throw new IllegalStateException("No such dependency " + m);
        }
        return (ModuleInfo) dep;
    }

    private Object data(ModuleInfo module) throws Exception {
        Method dataM = Class.forName("org.netbeans.Module", true, module.getClass().getClassLoader()).getDeclaredMethod("data");
        dataM.setAccessible(true);
        return dataM.invoke(module);
    }
}
