package org.jenkinsci.plugins.ivytrigger.service;

import hudson.FilePath;
import hudson.remoting.Callable;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class IvyTriggerEvaluator implements Callable<Map<String, String>, XTriggerException> {

    private FilePath ivyFilePath;

    private FilePath ivySettingsFilePath;

    private XTriggerLog log;

    public IvyTriggerEvaluator(FilePath ivyFilePath, FilePath ivySettingsFilePath, XTriggerLog log) {
        this.ivyFilePath = ivyFilePath;
        this.ivySettingsFilePath = ivySettingsFilePath;
        this.log = log;
    }

    public Map<String, String> call() throws XTriggerException {
        Map<String, String> result;
        try {
            log.info(String.format("Recording dependencies versions of the given Ivy path '%s'.", ivyFilePath));
            ResolveReport resolveReport;
            Ivy ivy = getIvyObject(log);
            resolveReport = ivy.resolve(new File(ivyFilePath.getRemote()));
            List dependencies = resolveReport.getDependencies();
            result = getMapDependencies(dependencies);
        } catch (ParseException pe) {
            throw new XTriggerException(pe);
        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }

        return result;
    }

    private Ivy getIvyObject(XTriggerLog log) throws XTriggerException {
        Ivy ivy = Ivy.newInstance();
        try {
            if (ivySettingsFilePath == null) {
                log.info("Ivy is configured using default 2.0 settings.");
                ivy.configureDefault();
            } else {
                log.info(String.format("Ivy is configured using the Ivy settings '%s'.", ivySettingsFilePath.getRemote()));
                ivy.configure(new File(ivySettingsFilePath.getRemote()));
            }
        } catch (ParseException pe) {
            throw new XTriggerException(pe);
        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }
        return ivy;
    }

    private Map<String, String> getMapDependencies(List dependencies) {
        Map<String, String> result = new HashMap<String, String>();
        for (Object dependencyObject : dependencies) {
            IvyNode dependencyNode = (IvyNode) dependencyObject;
            ModuleRevisionId moduleRevisionId = dependencyNode.getId();
            ResolvedModuleRevision resolvedModuleRevision = dependencyNode.getModuleRevision();
            if (resolvedModuleRevision != null) {
                String evaluatedRevision = resolvedModuleRevision.getId().getRevision();
                result.put(moduleRevisionId.toString(), evaluatedRevision);
            }
        }
        return result;
    }

}
