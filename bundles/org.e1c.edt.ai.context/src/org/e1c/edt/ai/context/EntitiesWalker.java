/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.StatisticsType;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
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
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EntitiesWalker
    implements IEntitiesWalker
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IIdFactory idFactory;

    @Inject
    public EntitiesWalker(ILog log, IV8Model v8Model, IIdFactory idFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        this.log = log;
        this.v8Model = v8Model;
        this.idFactory = idFactory;
    }

    @Override
    public boolean walk(String path, int start, int finish, IEntityVisitor visitor, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        try
        {
            ModuleInfo moduleInfo;
            try (var measurement = statistics.measureDuration(StatisticsType.LOAD_MODULE))
            {
                var optionalModuleInfo = v8Model.getModuleInfo(path, cancellationToken);
                if (optionalModuleInfo.isEmpty())
                {
                    return false;
                }

                moduleInfo = optionalModuleInfo.get();
            }

            var module = moduleInfo.getModule();
            var owner = module.getOwner();
            var bmModel = moduleInfo.getBmModel();
            while (owner != null)
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                if (owner instanceof Form)
                {
                    visitor.visitForm((Form)owner);
                }

                if (owner instanceof IBmObject)
                {
                    visitOwner(visitor, (IBmObject)owner);
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
                if (obj instanceof Variable || obj instanceof Invocation || obj instanceof FeatureAccess
                    || obj instanceof Method)
                {
                    var node = v8Model.getNode(obj);
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

                    if (obj instanceof Variable && visitor.visitVariable(nodeId, (Variable)obj, node))
                    {
                        traceVisit(obj, true);
                        return true;
                    }

                    if (obj instanceof Invocation && visitor.visitInvocation(nodeId, (Invocation)obj, node))
                    {
                        traceVisit(obj, true);
                        return true;
                    }

                    if (obj instanceof FeatureAccess && visitor.visitFeatureAccess(nodeId, (FeatureAccess)obj, node))
                    {
                        traceVisit(obj, true);
                        return true;
                    }

                    if (obj instanceof Method && visitor.visitMethod(nodeId, (Method)obj, node))
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

    private void visitOwner(IEntityVisitor visitor, IBmObject owner)
    {
        if (owner instanceof AccountingRegister)
        {
            var element = (AccountingRegister)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var resource : element.getResources())
            {
                visitor.visitOwnerResource(owner, resource);
            }

            for (var dimension : element.getDimensions())
            {
                visitor.visitOwnerDimension(owner, dimension);
            }
        }

        if (owner instanceof AccumulationRegister)
        {
            var element = (AccumulationRegister)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var resource : element.getResources())
            {
                visitor.visitOwnerResource(owner, resource);
            }

            for (var dimension : element.getDimensions())
            {
                visitor.visitOwnerDimension(owner, dimension);
            }
        }

        if (owner instanceof BusinessProcess)
        {
            var element = (BusinessProcess)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }
        }

        if (owner instanceof CalculationRegister)
        {
            var element = (CalculationRegister)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var resource : element.getResources())
            {
                visitor.visitOwnerResource(owner, resource);
            }

            for (var dimension : element.getDimensions())
            {
                visitor.visitOwnerDimension(owner, dimension);
            }
        }

        if (owner instanceof Catalog)
        {
            var element = (Catalog)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }
        }

        if (owner instanceof ChartOfAccounts)
        {
            var element = (ChartOfAccounts)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }
        }

        if (owner instanceof ChartOfCalculationTypes)
        {
            var element = (ChartOfCalculationTypes)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }
        }

        if (owner instanceof ChartOfCharacteristicTypes)
        {
            var element = (ChartOfCharacteristicTypes)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }
        }

        if (owner instanceof DataProcessor)
        {
            var element = (DataProcessor)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }
        }

        if (owner instanceof DataProcessorTabularSection)
        {
            var element = (DataProcessorTabularSection)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }
        }

        if (owner instanceof DbObjectTabularSection)
        {
            var element = (DbObjectTabularSection)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }
        }

        if (owner instanceof Document)
        {
            var element = (Document)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }

            // There's a lot of data here (IDEAI-134):
            /*for (var registerRecord : element.getRegisterRecords())
            {
                visitor.visitOwnerRegisterRecord(owner, registerRecord);
            }*/
        }

        if (owner instanceof ExchangePlan)
        {
            var element = (ExchangePlan)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }
        }

        if (owner instanceof ExternalDataProcessor)
        {
            var element = (ExternalDataProcessor)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }
        }

        if (owner instanceof ExternalReport)
        {
            var element = (ExternalReport)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }
        }

        if (owner instanceof InformationRegister)
        {
            var element = (InformationRegister)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var resource : element.getResources())
            {
                visitor.visitOwnerResource(owner, resource);
            }

            for (var dimension : element.getDimensions())
            {
                visitor.visitOwnerDimension(owner, dimension);
            }
        }

        if (owner instanceof Report)
        {
            var element = (Report)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }
        }

        if (owner instanceof ReportTabularSection)
        {
            var element = (ReportTabularSection)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }
        }

        if (owner instanceof Task)
        {
            var element = (Task)owner;
            for (var attr : element.getAttributes())
            {
                visitor.visitOwnerAttribute(owner, attr);
            }

            for (var tabularSection : element.getTabularSections())
            {
                visitor.visitOwnerTabularSection(owner, tabularSection);
            }
        }
    }

    private void traceVisit(EObject eObject, boolean visited)
    {
        /*System.out.println(visited + ", " + eObject.getClass().getName() + ": "
            + getNode(eObject).getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));*/
    }
}
