/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.IDocument;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
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
    private final IBmPovider bmPovider;

    @Inject
    public EntitiesWalker(ILog log, IV8Model v8Model, IIdFactory idFactory,
        IBmPovider bmPovider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(bmPovider);
        this.log = log;
        this.v8Model = v8Model;
        this.idFactory = idFactory;
        this.bmPovider = bmPovider;
    }

    @Override
    public boolean walk(IDocument document, String path, int start, int finish, IModuleProvider resourceSetProvider,
        IEntityVisitor visitor,
        IStatistics statistics, ICancellationToken cancellationToken)
    {
        try
        {
            Optional<BmRoot> optionalRoot;
            try (var measurement = statistics.measureDuration(StatisticsType.LOAD_MODULE_DURATUION))
            {
                optionalRoot = bmPovider.getRoot(path, cancellationToken);
                if (optionalRoot.isEmpty())
                {
                    return false;
                }
            }

            var root = optionalRoot.get();
            EObject nextObject = null;
            while (true)
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                if (nextObject == null)
                {
                    nextObject = root.getBmObject();
                }
                else
                {
                    var newOwner = nextObject.eContainer();
                    if (newOwner == null)
                    {
                        nextObject = v8Model.getBmObjectOwner(root.getModel(), nextObject);
                    }
                    else
                    {
                        nextObject = newOwner;
                    }

                    if (nextObject == null)
                    {
                        break;
                    }
                }

                if (nextObject instanceof Module)
                {
                    visitor.visitModule(root, (Module)nextObject);
                    var contentsIterator = nextObject.eAllContents();
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

                        visitor.visitNode(root, obj, node);
                        if (obj instanceof Variable || obj instanceof Invocation || obj instanceof FeatureAccess
                            || obj instanceof Method)
                        {
                            var nodeStart = node.getTotalOffset();
                            var nodeFinish = node.getTotalEndOffset();

                            if (!((nodeStart >= start && nodeStart <= finish)
                                || (nodeFinish >= start && nodeFinish <= finish)) && !(obj instanceof Method))
                            {
                                continue;
                            }

                            var nodeId = idFactory.createNodeId(path, node);
                            if (nodeId == null)
                            {
                                continue;
                            }

                            if (obj instanceof Method && visitor.visitMethod(root, nodeId, (Method)obj, node))
                            {
                                traceVisitEObject(obj, true);
                                return true;
                            }

                            if (!cancellationToken.isCanceled())
                            {
                                if (obj instanceof Variable && visitor.visitVariable(root, nodeId, (Variable)obj, node))
                                {
                                    traceVisitEObject(obj, true);
                                    return true;
                                }

                                if (obj instanceof Invocation
                                    && visitor.visitInvocation(root, nodeId, (Invocation)obj, node))
                                {
                                    traceVisitEObject(obj, true);
                                    return true;
                                }

                                if (obj instanceof FeatureAccess
                                    && visitor.visitFeatureAccess(root, nodeId, (FeatureAccess)obj, node))
                                {
                                    traceVisitEObject(obj, true);
                                    return true;
                                }
                            }
                        }

                        traceVisitEObject(obj, false);
                    }

                    continue;
                }

                if (nextObject instanceof Form)
                {
                    if (visitor.visitForm(root, (Form)nextObject))
                    {
                        return true;
                    }

                    continue;
                }

                if (!(nextObject instanceof IBmObject))
                {
                    continue;
                }

                var bmObject = (IBmObject)nextObject;
                if (visitor.visitBmObject(root, bmObject))
                {
                    return true;
                }

                if (bmObject instanceof AccountingRegister)
                {
                    var element = (AccountingRegister)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var resource : element.getResources())
                    {
                        if (visitor.visitResource(root, bmObject, resource))
                        {
                            return true;
                        }
                    }

                    for (var dimension : element.getDimensions())
                    {
                        if (visitor.visitDimension(root, bmObject, dimension))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof AccumulationRegister)
                {
                    var element = (AccumulationRegister)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var resource : element.getResources())
                    {
                        if (visitor.visitResource(root, bmObject, resource))
                        {
                            return true;
                        }
                    }

                    for (var dimension : element.getDimensions())
                    {
                        if (visitor.visitDimension(root, bmObject, dimension))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof BusinessProcess)
                {
                    var element = (BusinessProcess)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        if (visitor.visitTabularSection(root, bmObject, tabularSection))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof CalculationRegister)
                {
                    var element = (CalculationRegister)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var resource : element.getResources())
                    {
                        if (visitor.visitResource(root, bmObject, resource))
                        {
                            return true;
                        }
                    }

                    for (var dimension : element.getDimensions())
                    {
                        if (visitor.visitDimension(root, bmObject, dimension))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof Catalog)
                {
                    var element = (Catalog)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        if (visitor.visitTabularSection(root, bmObject, tabularSection))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof ChartOfAccounts)
                {
                    var element = (ChartOfAccounts)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        if (visitor.visitTabularSection(root, bmObject, tabularSection))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof ChartOfCalculationTypes)
                {
                    var element = (ChartOfCalculationTypes)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        if (visitor.visitTabularSection(root, bmObject, tabularSection))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof ChartOfCharacteristicTypes)
                {
                    var element = (ChartOfCharacteristicTypes)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        if (visitor.visitTabularSection(root, bmObject, tabularSection))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof DataProcessor)
                {
                    var element = (DataProcessor)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof DataProcessorTabularSection)
                {
                    var element = (DataProcessorTabularSection)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    continue;
                }

                if (bmObject instanceof DbObjectTabularSection)
                {
                    var element = (DbObjectTabularSection)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    continue;
                }

                if (bmObject instanceof Document)
                {
                    var element = (Document)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        visitor.visitTabularSection(root, bmObject, tabularSection);
                    }

                    // There's a lot of data here (IDEAI-134):
                    /*for (var registerRecord : element.getRegisterRecords())
                    {
                        if (visitor.visitOwnerRegisterRecord(root, bmObject, registerRecord))
                        {
                            return true;
                        }
                    }*/

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof ExchangePlan)
                {
                    var element = (ExchangePlan)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        if (visitor.visitTabularSection(root, bmObject, tabularSection))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof ExternalDataProcessor)
                {
                    var element = (ExternalDataProcessor)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof ExternalReport)
                {
                    var element = (ExternalReport)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof InformationRegister)
                {
                    var element = (InformationRegister)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var resource : element.getResources())
                    {
                        if (visitor.visitResource(root, bmObject, resource))
                        {
                            return true;
                        }
                    }

                    for (var dimension : element.getDimensions())
                    {
                        if (visitor.visitDimension(root, bmObject, dimension))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof Report)
                {
                    var element = (Report)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof ReportTabularSection)
                {
                    var element = (ReportTabularSection)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    continue;
                }

                if (bmObject instanceof Task)
                {
                    var element = (Task)bmObject;
                    for (var attr : element.getAttributes())
                    {
                        if (visitor.visitAttribute(root, bmObject, attr))
                        {
                            return true;
                        }
                    }

                    for (var tabularSection : element.getTabularSections())
                    {
                        if (visitor.visitTabularSection(root, bmObject, tabularSection))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }

                if (bmObject instanceof com._1c.g5.v8.dt.metadata.mdclass.Enum)
                {
                    var element = (com._1c.g5.v8.dt.metadata.mdclass.Enum)bmObject;
                    for (var val : element.getEnumValues())
                    {
                        if (visitor.visitEnumValue(root, bmObject, val))
                        {
                            return true;
                        }
                    }

                    for (var form : element.getForms())
                    {
                        visitor.visitForm(root, bmObject, form);
                    }

                    continue;
                }
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return false;
        }

        return true;
    }

    private void traceVisitEObject(EObject eObject, boolean visited)
    {
        /*System.out.println(visited + ", " + eObject.getClass().getName() + ": "
            + getNode(eObject).getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));*/
    }
}
