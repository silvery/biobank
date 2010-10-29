package edu.ualberta.med.biobank.treeview.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import edu.ualberta.med.biobank.common.wrappers.ModelWrapper;
import edu.ualberta.med.biobank.treeview.AdapterBase;

public class AdapterFactory {

    private static List<String> adaptersPackages;

    static {
        String topPackage = AdapterBase.class.getPackage().getName();
        adaptersPackages = new ArrayList<String>();
        for (Package p : Package.getPackages()) {
            if (p.getName().startsWith(topPackage)) {
                adaptersPackages.add(p.getName());
            }
        }
    }

    public static AdapterBase getAdapter(ModelWrapper<?> wrapper) {
        Class<?> wrapperClass = wrapper.getClass();
        String wrapperClassName = wrapperClass.getSimpleName();
        String adapterClassName = wrapperClassName
            .replace("Wrapper", "Adapter");
        try {
            for (String packageName : adaptersPackages) {
                Class<?> klass;
                try {
                    klass = Class.forName(packageName + "." + adapterClassName);
                } catch (ClassNotFoundException e) {
                    // try next package
                    continue;
                }
                Constructor<?> constructor = klass.getConstructor(new Class[] {
                    AdapterBase.class, wrapperClass });
                return (AdapterBase) constructor.newInstance(new Object[] {
                    null, wrapper });
            }
            throw new Exception("No adapter class found");
        } catch (Exception e) {
            throw new RuntimeException(
                "error in invoking adapter for wrapper: " + wrapperClassName
                    + " (adapter name is " + adapterClassName + "). ", e);
        }
    }
}