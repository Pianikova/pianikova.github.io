/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.AccountingRegister;
import com._1c.g5.v8.dt.metadata.mdclass.AccumulationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.BusinessProcess;
import com._1c.g5.v8.dt.metadata.mdclass.CalculationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Catalog;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfAccounts;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCalculationTypes;
import com._1c.g5.v8.dt.metadata.mdclass.ChartOfCharacteristicTypes;
import com._1c.g5.v8.dt.metadata.mdclass.DataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.DataProcessorTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.Document;
import com._1c.g5.v8.dt.metadata.mdclass.ExchangePlan;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalDataProcessor;
import com._1c.g5.v8.dt.metadata.mdclass.ExternalReport;
import com._1c.g5.v8.dt.metadata.mdclass.InformationRegister;
import com._1c.g5.v8.dt.metadata.mdclass.Report;
import com._1c.g5.v8.dt.metadata.mdclass.ReportTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.Task;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.StatisticsType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class EntitiesWalker
    implements IEntitiesWalker
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IIdFactory idFactory;
    private final IBmModelManager modelManager;
    private final IResourceLookup resourceLookup;

    @Inject
    public EntitiesWalker(ILog log, IV8Model v8Model, IIdFactory idFactory,
        IBmModelManager modelManager, IResourceLookup resourceLookup)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(resourceLookup);
        this.log = log;
        this.v8Model = v8Model;
        this.idFactory = idFactory;
        this.modelManager = modelManager;
        this.resourceLookup = resourceLookup;
    }

    @Override
    public boolean walk(String path, int start, int finish, IModuleProvider resourceSetProvider, IEntityVisitor visitor,
        IStatistics statistics, ICancellationToken cancellationToken)
    {
        try
        {
            ModuleInfo moduleInfo;
            try (var measurement = statistics.measureDuration(StatisticsType.LOAD_MODULE_DURATUION))
            {
                var optionalModuleInfo = resourceSetProvider.getModule(path, cancellationToken);
                if (optionalModuleInfo.isEmpty())
                {
                    return false;
                }

                moduleInfo = optionalModuleInfo.get();
            }

            visitor.visitModule(moduleInfo);
            var module = moduleInfo.getModule();
            var owner = module.getOwner();
            var project = resourceLookup.getProject(module);
            IBmModel bmModel = null;
            if (project != null)
            {
                bmModel = modelManager.getModel(project);
            }

            while (owner != null)
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                if (owner instanceof Form)
                {
                    if (visitor.visitForm(moduleInfo, (Form)owner))
                    {
                        return true;
                    }
                }

                if (owner instanceof IBmObject)
                {
                    visitOwner(moduleInfo, visitor, (IBmObject)owner);
                }

                var newOwner = owner.eContainer();
                if (newOwner == null && bmModel != null)
                {
                    owner = v8Model.getBmObjectOwner(bmModel, owner);
                }
                else
                {
                    owner = newOwner;
                }
            }

            var contentsIterator = module.eAllContents();
            while (contentsIterator.hasNext())
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                var obj = contentsIterator.next();
                var node = v8Model.getNode(obj);
                if (node == null)
                {
                    continue;
                }

                if (obj instanceof Variable || obj instanceof Invocation || obj instanceof FeatureAccess
                    || obj instanceof Method)
                {
                    var nodeStart = node.getTotalOffset();
                    var nodeFinish = node.getTotalEndOffset();

                    if (!((nodeStart >= start && nodeStart <= finish) || (nodeFinish >= start && nodeFinish <= finish))
                        && !(obj instanceof Method))
                    {
                        continue;
                    }

                    var nodeId = idFactory.createNodeId(path, node);
                    if (nodeId == null)
                    {
                        continue;
                    }

                    if (obj instanceof Variable && visitor.visitVariable(moduleInfo, nodeId, (Variable)obj, node))
                    {
                        traceVisit(obj, true);
                        return true;
                    }

                    if (obj instanceof Invocation && visitor.visitInvocation(moduleInfo, nodeId, (Invocation)obj, node))
                    {
                        traceVisit(obj, true);
                        return true;
                    }

                    if (obj instanceof FeatureAccess
                        && visitor.visitFeatureAccess(moduleInfo, nodeId, (FeatureAccess)obj, node))
                    {
                        traceVisit(obj, true);
                        return true;
                    }

                    if (obj instanceof Method && visitor.visitMethod(moduleInfo, nodeId, (Method)obj, node))
                    {
                        traceVisit(obj, true);
                        return true;
                    }
                }

                traceVisit(obj, false);
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return false;
        }

        return true;
    }

    private boolean visitOwner(ModuleInfo moduleInfo, IEntityVisitor visitor, IBmObject owner)
    {
        if (visitor.visitOwner(moduleInfo, owner))
        {
            return true;
        }

        if (owner instanceof AccountingRegister)
        {
            var element = (AccountingRegister)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var resource : element.getResources())
            {
                if (visitor.visitOwnerResource(moduleInfo, owner, resource))
                {
                    return true;
                }
            }

            for (var dimension : element.getDimensions())
            {
                if (visitor.visitOwnerDimension(moduleInfo, owner, dimension))
                {
                    return true;
                }
            }
        }

        if (owner instanceof AccumulationRegister)
        {
            var element = (AccumulationRegister)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var resource : element.getResources())
            {
                if (visitor.visitOwnerResource(moduleInfo, owner, resource))
                {
                    return true;
                }
            }

            for (var dimension : element.getDimensions())
            {
                if (visitor.visitOwnerDimension(moduleInfo, owner, dimension))
                {
                    return true;
                }
            }
        }

        if (owner instanceof BusinessProcess)
        {
            var element = (BusinessProcess)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                if (visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection))
                {
                    return true;
                }
            }
        }

        if (owner instanceof CalculationRegister)
        {
            var element = (CalculationRegister)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var resource : element.getResources())
            {
                if (visitor.visitOwnerResource(moduleInfo, owner, resource))
                {
                    return true;
                }
            }

            for (var dimension : element.getDimensions())
            {
                if (visitor.visitOwnerDimension(moduleInfo, owner, dimension))
                {
                    return true;
                }
            }
        }

        if (owner instanceof Catalog)
        {
            var element = (Catalog)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                if (visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection))
                {
                    return true;
                }
            }
        }

        if (owner instanceof ChartOfAccounts)
        {
            var element = (ChartOfAccounts)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                if (visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection))
                {
                    return true;
                }
            }
        }

        if (owner instanceof ChartOfCalculationTypes)
        {
            var element = (ChartOfCalculationTypes)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                if (visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection))
                {
                    return true;
                }
            }
        }

        if (owner instanceof ChartOfCharacteristicTypes)
        {
            var element = (ChartOfCharacteristicTypes)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                if (visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection))
                {
                    return true;
                }
            }
        }

        if (owner instanceof DataProcessor)
        {
            var element = (DataProcessor)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }
        }

        if (owner instanceof DataProcessorTabularSection)
        {
            var element = (DataProcessorTabularSection)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }
        }

        if (owner instanceof DbObjectTabularSection)
        {
            var element = (DbObjectTabularSection)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }
        }

        if (owner instanceof Document)
        {
            var element = (Document)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection);
            }

            // There's a lot of data here (IDEAI-134):
            /*for (var registerRecord : element.getRegisterRecords())
            {
                if (visitor.visitOwnerRegisterRecord(moduleInfo, owner, registerRecord))
                {
                    return true;
                }
            }*/
        }

        if (owner instanceof ExchangePlan)
        {
            var element = (ExchangePlan)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                if (visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection))
                {
                    return true;
                }
            }
        }

        if (owner instanceof ExternalDataProcessor)
        {
            var element = (ExternalDataProcessor)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }
        }

        if (owner instanceof ExternalReport)
        {
            var element = (ExternalReport)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }
        }

        if (owner instanceof InformationRegister)
        {
            var element = (InformationRegister)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var resource : element.getResources())
            {
                if (visitor.visitOwnerResource(moduleInfo, owner, resource))
                {
                    return true;
                }
            }

            for (var dimension : element.getDimensions())
            {
                if (visitor.visitOwnerDimension(moduleInfo, owner, dimension))
                {
                    return true;
                }
            }
        }

        if (owner instanceof Report)
        {
            var element = (Report)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }
        }

        if (owner instanceof ReportTabularSection)
        {
            var element = (ReportTabularSection)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }
        }

        if (owner instanceof Task)
        {
            var element = (Task)owner;
            for (var attr : element.getAttributes())
            {
                if (visitor.visitOwnerAttribute(moduleInfo, owner, attr))
                {
                    return true;
                }
            }

            for (var tabularSection : element.getTabularSections())
            {
                if (visitor.visitOwnerTabularSection(moduleInfo, owner, tabularSection))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private void traceVisit(EObject eObject, boolean visited)
    {
        /*System.out.println(visited + ", " + eObject.getClass().getName() + ": "
            + getNode(eObject).getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));*/
    }
}
