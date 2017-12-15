package edu.illinois.starts.jdeps;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by randy on 10/14/17.
 */
public class GradleClassLoader  extends URLClassLoader {
    private final ClassLoader parent = ClassLoader.getSystemClassLoader();
    private final Set<URL> urls = new HashSet();
    private final String roleName;
    private boolean childDelegation = true;
    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    public GradleClassLoader(ClassLoader parent, String roleName) {
        super(EMPTY_URL_ARRAY, parent);
        this.roleName = roleName;
    }

    public void addURL(URL url) {
        if(!this.urls.contains(url)) {
            super.addURL(url);
            this.urls.add(url);
        }
    }

    public void addURL(File file) throws MalformedURLException {
        addURL(file.toURI().toURL());
    }

    public synchronized Class loadClass(String name) throws ClassNotFoundException {
        Class c;
        if(this.childDelegation) {
            c = this.findLoadedClass(name);
            ClassNotFoundException ex = null;
            if(c == null) {
                try {
                    c = this.findClass(name);
                } catch (ClassNotFoundException var5) {
                    ex = var5;
                    if(this.parent != null) {
                        c = this.parent.loadClass(name);
                    }
                }
            }
            if(c == null) {
                throw ex;
            }
        } else {
            c = super.loadClass(name);
        }
        return c;
    }

    public String toString() {
        return "GradleClassLoader{roleName=\'" + this.roleName + "\'}";
    }
}
