package io.servicecat.dependencies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MavenParentPomDependencyReader implements DependencyReader {

    private String defaultLicenseFile = "target/generated-resources/licenses.xml";
    private List<String> nonMavenDirs = new ArrayList<>();

    @Autowired
    MavenDependencyReader mavenDependencyReader;

    public MavenParentPomDependencyReader() {
        nonMavenDirs.add("src");
        nonMavenDirs.add("target");
        nonMavenDirs.add(".svn");
        nonMavenDirs.add(".git");
        nonMavenDirs.add("vagrant");
    }

    @Override
    public List<Dependency> readDependencies(String sourceRootDirectory) {
        List<Dependency> dependencies = new ArrayList<>();
        try {
            String licenseFile = sourceRootDirectory + "/" + defaultLicenseFile;
            dependencies = mavenDependencyReader.readDependencies(new File(licenseFile));
            dependencies.addAll(readDependenciesFromSubmodules(sourceRootDirectory));
        } catch (Exception e) {
            System.out.println("The file " + defaultLicenseFile + " could not be read.");
        }
        return removeDuplicates(dependencies);
    }

    public List<Dependency> readDependenciesFromSubmodules(String sourceDir) {
        File directory = new File(sourceDir);
        File[] files = directory.listFiles();
        List<Dependency> allSubDependencies = new ArrayList<>();
        for (File file : files) {
            if (isPotentialMavenSubModuleDirectory(file)) {
                List<Dependency> subDependencies = mavenDependencyReader.readDependencies(new File(file.getAbsolutePath() + "/" + defaultLicenseFile));
                allSubDependencies.addAll(subDependencies);
            }
        }
        return allSubDependencies;
    }

    private boolean isPotentialMavenSubModuleDirectory(File file) {
        return file.isDirectory() && !nonMavenDirs.contains(file.getName());
    }

    List<Dependency> removeDuplicates(List<Dependency> dependencies) {
        List<Dependency> clean = new ArrayList<>();
        dependencies.forEach(dependency -> {
            if (!clean.contains(dependency)) {
                clean.add(dependency);
            }
        });
        Collections.sort(clean);
        return clean;
    }
}
