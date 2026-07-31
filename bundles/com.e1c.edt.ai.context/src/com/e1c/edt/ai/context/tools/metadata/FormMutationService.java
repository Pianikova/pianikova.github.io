/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.AbstractBmTask;
import com._1c.g5.v8.dt.core.naming.ITopObjectFqnGenerator;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.model.AbstractDataPath;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormAttribute;
import com._1c.g5.v8.dt.form.model.FormCommand;
import com._1c.g5.v8.dt.form.model.FormFactory;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormGroup;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.FormItemContainer;
import com._1c.g5.v8.dt.form.model.FormParameter;
import com._1c.g5.v8.dt.form.model.ManagedFormFieldType;
import com._1c.g5.v8.dt.form.model.ManagedFormGroupType;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.form.model.Titled;
import com._1c.g5.v8.dt.form.service.FormIdentifierService;
import com._1c.g5.v8.dt.form.service.item.FormNewItemDescriptor;
import com._1c.g5.v8.dt.form.service.item.IFormItemManagementService;
import com._1c.g5.v8.dt.form.service.item.IFormItemMovementService;
import com._1c.g5.v8.dt.form.service.item.IFormItemTypeManagementService;
import com._1c.g5.v8.dt.mcore.Command;
import com._1c.g5.v8.dt.mcore.NamedElement;
import com._1c.g5.v8.dt.mcore.TypeDescription;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.metadata.mdclass.BasicForm;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Editor of an <em>existing</em> form: its attributes, commands, parameters, and the item tree.
 * <p>
 * Form content lives in the external {@code Form.form} body, not in the owner {@code .mdo}, and EDT
 * locks that file against direct text editing. Everything here therefore goes through the native EDT
 * form services inside a BM transaction, which is the only supported way to change a form.
 */
@Singleton
final class FormMutationService
{
    private final IBmModelManager modelManager;
    private final ITopObjectFqnGenerator fqnGenerator;
    private final IV8ProjectManager v8ProjectManager;
    private final MetadataTypeService typeService;
    private final IFormItemManagementService itemManagementService;
    private final IFormItemMovementService itemMovementService;
    private final IFormItemTypeManagementService itemTypeManagementService;

    @Inject
    FormMutationService(IBmModelManager modelManager, ITopObjectFqnGenerator fqnGenerator,
        IV8ProjectManager v8ProjectManager, MetadataTypeService typeService,
        IFormItemManagementService itemManagementService, IFormItemMovementService itemMovementService,
        IFormItemTypeManagementService itemTypeManagementService)
    {
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(fqnGenerator);
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(typeService);
        Preconditions.checkNotNull(itemManagementService);
        Preconditions.checkNotNull(itemMovementService);
        Preconditions.checkNotNull(itemTypeManagementService);
        this.modelManager = modelManager;
        this.fqnGenerator = fqnGenerator;
        this.v8ProjectManager = v8ProjectManager;
        this.typeService = typeService;
        this.itemManagementService = itemManagementService;
        this.itemMovementService = itemMovementService;
        this.itemTypeManagementService = itemTypeManagementService;
    }

    /** Operations this service handles; {@code object_name} is always a form FQN. */
    static final java.util.Set<String> OPERATIONS = java.util.Set.of("inspectForm", "addFormAttribute", //$NON-NLS-1$ //$NON-NLS-2$
        "removeFormAttribute", "addFormField", "addFormGroup", "addFormButton", "addFormCommand", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "removeFormCommand", "removeFormItem", "moveFormItem", "setFormItemProperty", "setFormProperty"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    MetadataResponse execute(IProject project, MetadataRequest request)
    {
        switch (request.operation)
        {
        case "inspectForm": //$NON-NLS-1$
            return inspectForm(project, request);
        case "addFormAttribute": //$NON-NLS-1$
            return addFormAttribute(project, request);
        case "removeFormAttribute": //$NON-NLS-1$
            return removeFormAttribute(project, request);
        case "addFormField": //$NON-NLS-1$
            return addFormField(project, request);
        case "addFormGroup": //$NON-NLS-1$
            return addFormGroup(project, request);
        case "addFormButton": //$NON-NLS-1$
            return addFormButton(project, request);
        case "addFormCommand": //$NON-NLS-1$
            return addFormCommand(project, request);
        case "removeFormCommand": //$NON-NLS-1$
            return removeFormCommand(project, request);
        case "removeFormItem": //$NON-NLS-1$
            return removeFormItem(project, request);
        case "moveFormItem": //$NON-NLS-1$
            return moveFormItem(project, request);
        case "setFormItemProperty": //$NON-NLS-1$
            return setFormItemProperty(project, request);
        case "setFormProperty": //$NON-NLS-1$
            return setFormProperty(project, request);
        default:
            throw new ToolException("Operation is not a form operation: " + request.operation); //$NON-NLS-1$
        }
    }

    // ---------------------------------------------------------------- inspect

    private MetadataResponse inspectForm(IProject project, MetadataRequest request)
    {
        var result = new LinkedHashMap<String, Object>();
        read(project, request, (transaction, form) -> {
            result.put("form", request.objectName); //$NON-NLS-1$
            result.put("title", localized(form.getTitle())); //$NON-NLS-1$
            var attributes = new ArrayList<Object>();
            for (var attribute : form.getAttributes())
            {
                var item = new LinkedHashMap<String, Object>();
                item.put("name", attribute.getName()); //$NON-NLS-1$
                item.put("type", typeNames(attribute.getValueType())); //$NON-NLS-1$
                item.put("main", attribute.isMain()); //$NON-NLS-1$
                var columns = new ArrayList<String>();
                for (var column : attribute.getColumns())
                {
                    columns.add(column.getName());
                }
                if (!columns.isEmpty())
                {
                    item.put("columns", columns); //$NON-NLS-1$
                }
                attributes.add(item);
            }
            result.put("attributes", attributes); //$NON-NLS-1$

            var commands = new ArrayList<Object>();
            for (var command : form.getFormCommands())
            {
                var item = new LinkedHashMap<String, Object>();
                item.put("name", command.getName()); //$NON-NLS-1$
                item.put("title", localized(command.getTitle())); //$NON-NLS-1$
                item.put("handler", handlerName(command)); //$NON-NLS-1$
                commands.add(item);
            }
            result.put("commands", commands); //$NON-NLS-1$

            var parameters = new ArrayList<Object>();
            for (FormParameter parameter : form.getParameters())
            {
                var item = new LinkedHashMap<String, Object>();
                item.put("name", parameter.getName()); //$NON-NLS-1$
                item.put("type", typeNames(parameter.getValueType())); //$NON-NLS-1$
                parameters.add(item);
            }
            result.put("parameters", parameters); //$NON-NLS-1$
            result.put("items", describeItems(form)); //$NON-NLS-1$
            return null;
        });
        var response = MetadataResponse.success(request, request.objectName, false);
        response.details = result;
        return response;
    }

    private static List<Object> describeItems(FormItemContainer container)
    {
        var result = new ArrayList<Object>();
        for (var item : container.getItems())
        {
            var described = new LinkedHashMap<String, Object>();
            described.put("name", item.getName()); //$NON-NLS-1$
            described.put("kind", item.eClass().getName()); //$NON-NLS-1$
            if (item instanceof FormField)
            {
                var field = (FormField)item;
                described.put("type", field.getType() != null ? field.getType().getName() : null); //$NON-NLS-1$
                described.put("data_path", pathOf(field.getDataPath())); //$NON-NLS-1$
            }
            else if (item instanceof FormGroup)
            {
                described.put("type", ((FormGroup)item).getType() != null //$NON-NLS-1$
                    ? ((FormGroup)item).getType().getName() : null);
            }
            else if (item instanceof Button)
            {
                var button = (Button)item;
                described.put("type", button.getType() != null ? button.getType().getName() : null); //$NON-NLS-1$
                described.put("command", button.getCommandName() instanceof NamedElement //$NON-NLS-1$
                    ? ((NamedElement)button.getCommandName()).getName() : null);
            }
            else if (item instanceof Table)
            {
                described.put("data_path", pathOf(((Table)item).getDataPath())); //$NON-NLS-1$
            }
            if (item instanceof Titled)
            {
                described.put("title", localized(((Titled)item).getTitle())); //$NON-NLS-1$
            }
            if (item instanceof FormItemContainer)
            {
                var children = describeItems((FormItemContainer)item);
                if (!children.isEmpty())
                {
                    described.put("children", children); //$NON-NLS-1$
                }
            }
            result.add(described);
        }
        return result;
    }

    // ------------------------------------------------------------- attributes

    private MetadataResponse addFormAttribute(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        boolean[] changed = { false };
        write(project, request, "Add 1C form attribute", (transaction, form) -> { //$NON-NLS-1$
            if (findAttribute(form, request.name) != null)
            {
                throw new ToolException("Form attribute already exists: " + request.name //$NON-NLS-1$
                    + ". Use removeFormAttribute first, or pick another name."); //$NON-NLS-1$
            }
            TypeDescription valueType = typeService.create(project, transaction, request);
            changed[0] = true;
            if (request.dryRun)
            {
                return null;
            }
            var attribute = FormFactory.eINSTANCE.createFormAttribute();
            attribute.setName(request.name);
            attribute.setId(FormIdentifierService.INSTANCE.getNextAttributeId(form));
            attribute.setValueType(valueType);
            attribute.setView(alwaysAdjustable());
            attribute.setEdit(alwaysAdjustable());
            title(attribute.getTitle(), project, request, request.name);
            if (request.mainEnabled())
            {
                attribute.setMain(true);
            }
            form.getAttributes().add(attribute);
            return null;
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse removeFormAttribute(IProject project, MetadataRequest request)
    {
        boolean[] changed = { false };
        write(project, request, "Remove 1C form attribute", (transaction, form) -> { //$NON-NLS-1$
            var attribute = findAttribute(form, request.name);
            if (attribute == null)
            {
                throw new ToolException("Form attribute not found: " + request.name //$NON-NLS-1$
                    + ". Call inspectForm to list the existing ones."); //$NON-NLS-1$
            }
            changed[0] = true;
            if (!request.dryRun)
            {
                EcoreUtil.delete(attribute, true);
            }
            return null;
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    // ------------------------------------------------------------------ items

    private MetadataResponse addFormField(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        if (request.dataPath == null || request.dataPath.isBlank())
        {
            throw new ToolException("Parameter `data_path` is required for addFormField, for example" //$NON-NLS-1$
                + " Объект.Наименование or the name of a form attribute. Call inspectForm to see the" //$NON-NLS-1$
                + " attributes this form exposes."); //$NON-NLS-1$
        }
        boolean[] changed = { false };
        write(project, request, "Add 1C form field", (transaction, form) -> { //$NON-NLS-1$
            var parent = requireContainer(form, request.parent);
            requireFreeItemName(form, request.name);
            changed[0] = true;
            if (request.dryRun)
            {
                return null;
            }
            var dataPath = dataPath(request.dataPath);
            FormField field;
            try
            {
                field = itemManagementService.addField(parent, dataPath, position(request), form,
                    descriptor(project, request));
            }
            catch (RuntimeException e)
            {
                throw new ToolException("EDT could not bind a field to data path `" + request.dataPath //$NON-NLS-1$
                    + "`: " + rootCause(e) + ". The path must address an existing form attribute or one of" //$NON-NLS-1$ //$NON-NLS-2$
                    + " its properties; inspectForm lists them.", e, ToolErrorType.USER_VISIBLE); //$NON-NLS-1$
            }
            if (field == null)
            {
                throw new ToolException("EDT returned no field for data path `" + request.dataPath + "`."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (request.itemType != null && !request.itemType.isBlank())
            {
                itemTypeManagementService.setType(fieldType(request.itemType), field, field.getDataPath(), parent, form,
                    v8Version(project));
            }
            field.setName(request.name);
            title(field.getTitle(), project, request, null);
            return null;
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse addFormGroup(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        var groupType = groupType(request.groupType);
        boolean[] changed = { false };
        write(project, request, "Add 1C form group", (transaction, form) -> { //$NON-NLS-1$
            var parent = requireContainer(form, request.parent);
            requireFreeItemName(form, request.name);
            changed[0] = true;
            if (request.dryRun)
            {
                return null;
            }
            var group = itemManagementService.addGroup(parent, position(request), groupType, form,
                descriptor(project, request));
            if (group == null)
            {
                throw new ToolException("EDT returned no group for group_type " + groupType.getName() + "."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            group.setName(request.name);
            title(group.getTitle(), project, request, null);
            return null;
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse addFormButton(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        if (request.commandName == null || request.commandName.isBlank())
        {
            throw new ToolException("Parameter `command_name` is required for addFormButton: it names the" //$NON-NLS-1$
                + " form command the button runs. Create it first with addFormCommand."); //$NON-NLS-1$
        }
        boolean[] changed = { false };
        write(project, request, "Add 1C form button", (transaction, form) -> { //$NON-NLS-1$
            var parent = requireContainer(form, request.parent);
            requireFreeItemName(form, request.name);
            Command command = findCommand(form, request.commandName);
            if (command == null)
            {
                throw new ToolException("Form command not found: " + request.commandName //$NON-NLS-1$
                    + ". Create it with addFormCommand first; inspectForm lists the existing commands."); //$NON-NLS-1$
            }
            changed[0] = true;
            if (request.dryRun)
            {
                return null;
            }
            var button = itemManagementService.addButton(parent, position(request), command, null, form,
                descriptor(project, request));
            if (button == null)
            {
                throw new ToolException("EDT returned no button for command " + request.commandName + "."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            button.setName(request.name);
            title(button.getTitle(), project, request, null);
            return null;
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse removeFormItem(IProject project, MetadataRequest request)
    {
        boolean[] changed = { false };
        write(project, request, "Remove 1C form item", (transaction, form) -> { //$NON-NLS-1$
            var item = findItem(form, request.name);
            if (item == null)
            {
                throw new ToolException("Form item not found: " + request.name //$NON-NLS-1$
                    + ". Call inspectForm to list the item tree."); //$NON-NLS-1$
            }
            changed[0] = true;
            if (!request.dryRun)
            {
                EcoreUtil.delete(item, true);
            }
            return null;
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    private MetadataResponse moveFormItem(IProject project, MetadataRequest request)
    {
        boolean[] changed = { false };
        write(project, request, "Move 1C form item", (transaction, form) -> { //$NON-NLS-1$
            var item = findItem(form, request.name);
            if (item == null)
            {
                throw new ToolException("Form item not found: " + request.name //$NON-NLS-1$
                    + ". Call inspectForm to list the item tree."); //$NON-NLS-1$
            }
            var parent = requireContainer(form, request.parent);
            changed[0] = true;
            if (request.dryRun)
            {
                return null;
            }
            boolean moved = request.position != null
                ? itemMovementService.move(item, parent, request.position.intValue())
                : itemMovementService.moveToEnd(item, parent);
            if (!moved)
            {
                throw new ToolException("EDT refused to move `" + request.name + "` into `" //$NON-NLS-1$ //$NON-NLS-2$
                    + (request.parent == null ? "<form root>" : request.parent) //$NON-NLS-1$
                    + "`: that container does not accept this item kind."); //$NON-NLS-1$
            }
            return null;
        });
        return MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
    }

    // --------------------------------------------------------------- commands

    private MetadataResponse addFormCommand(IProject project, MetadataRequest request)
    {
        validateIdentifier(request.name, "name"); //$NON-NLS-1$
        boolean[] changed = { false };
        var details = new LinkedHashMap<String, Object>();
        var warnings = new ArrayList<String>();
        var modulePath = MetadataMutationService.formModulePath(project, request.objectName);
        write(project, request, "Add 1C form command", (transaction, form) -> { //$NON-NLS-1$
            if (findCommand(form, request.name) != null)
            {
                throw new ToolException("Form command already exists: " + request.name + "."); //$NON-NLS-1$ //$NON-NLS-2$
            }
            var handlerName = request.handler != null && !request.handler.isBlank() ? request.handler : request.name;
            validateIdentifier(handlerName, "handler"); //$NON-NLS-1$
            changed[0] = true;
            details.put("handler", handlerName); //$NON-NLS-1$
            details.put("handler_module_path", modulePath); //$NON-NLS-1$
            // A 1C form command must have a handler procedure, so until the BSL exists the form carries
            // an error marker. Said only in details it gets skipped, and the model reports success on a
            // broken form; as a warning it lands where the model already looks.
            warnings.add("The form command is not complete until its handler exists: write the procedure `" //$NON-NLS-1$
                + handlerName + "(Команда)` (Russian script variant) or `" + handlerName //$NON-NLS-1$ //$NON-NLS-2$
                + "(Command)` into " + modulePath + " with the Write or Edit tool, marked &НаКлиенте/&AtClient." //$NON-NLS-1$ //$NON-NLS-2$
                + " Until then the form has an error marker about the missing handler."); //$NON-NLS-1$
            if (request.dryRun)
            {
                return null;
            }
            var command = FormFactory.eINSTANCE.createFormCommand();
            command.setName(request.name);
            command.setId(FormIdentifierService.INSTANCE.getNextCommandId(form));
            // «Использование» is mandatory on a form command: EDT's own form templates always write it,
            // and a command without it is reported as an SU46 error.
            command.setUse(alwaysAdjustable());
            title(command.getTitle(), project, request, request.name);
            var handler = FormFactory.eINSTANCE.createCommandHandler();
            handler.setName(handlerName);
            var action = FormFactory.eINSTANCE.createFormCommandHandlerContainer();
            action.setHandler(handler);
            command.setAction(action);
            form.getFormCommands().add(command);
            return null;
        });
        var response = MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
        response.details = details;
        response.warnings.addAll(warnings);
        return response;
    }

    private MetadataResponse removeFormCommand(IProject project, MetadataRequest request)
    {
        boolean[] changed = { false };
        var warnings = new ArrayList<String>();
        write(project, request, "Remove 1C form command", (transaction, form) -> { //$NON-NLS-1$
            var command = findCommand(form, request.name);
            if (command == null)
            {
                throw new ToolException("Form command not found: " + request.name //$NON-NLS-1$
                    + ". Call inspectForm to list the existing commands."); //$NON-NLS-1$
            }
            // A button whose command is gone is invalid metadata, so the buttons go with the command
            // rather than being left behind pointing at nothing.
            var orphans = new ArrayList<FormItem>();
            for (var item : allItems(form))
            {
                if (item instanceof Button && ((Button)item).getCommandName() == command)
                {
                    orphans.add(item);
                    warnings.add("Button `" + item.getName() + "` ran this command and was removed with it."); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            changed[0] = true;
            if (!request.dryRun)
            {
                for (var orphan : orphans)
                {
                    EcoreUtil.delete(orphan, true);
                }
                EcoreUtil.delete(command, true);
            }
            return null;
        });
        var response = MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
        response.warnings.addAll(warnings);
        return response;
    }

    // ------------------------------------------------------------- properties

    private MetadataResponse setFormItemProperty(IProject project, MetadataRequest request)
    {
        if ("name".equalsIgnoreCase(request.propertyName)) //$NON-NLS-1$
        {
            throw new ToolException("A form element cannot be renamed in place: its name is what data paths," //$NON-NLS-1$
                + " buttons and the form module refer to. Remove it and add it again under the new name."); //$NON-NLS-1$
        }
        boolean[] changed = { false };
        var details = new LinkedHashMap<String, Object>();
        write(project, request, "Set 1C form item property", (transaction, form) -> { //$NON-NLS-1$
            EObject target = findItem(form, request.name);
            if (target == null)
            {
                target = findAttribute(form, request.name);
            }
            if (target == null)
            {
                target = findCommand(form, request.name);
            }
            if (target == null)
            {
                throw new ToolException("Form item, attribute or command not found: " + request.name //$NON-NLS-1$
                    + ". Call inspectForm to list them."); //$NON-NLS-1$
            }
            changed[0] = MetadataPropertyWriter.set(target, request, details);
            return null;
        });
        var response = MetadataResponse.success(request, request.objectName + "." + request.name, changed[0]); //$NON-NLS-1$
        response.details = details;
        return response;
    }

    private MetadataResponse setFormProperty(IProject project, MetadataRequest request)
    {
        boolean[] changed = { false };
        var details = new LinkedHashMap<String, Object>();
        write(project, request, "Set 1C form property", (transaction, form) -> { //$NON-NLS-1$
            changed[0] = MetadataPropertyWriter.set(form, request, details);
            return null;
        });
        var response = MetadataResponse.success(request, request.objectName, changed[0]);
        response.details = details;
        return response;
    }

    // ------------------------------------------------------------ form access

    /** A unit of work executed against the resolved {@link Form} inside a BM transaction. */
    private interface FormTask
    {
        Void run(IBmTransaction transaction, Form form);
    }

    private void write(IProject project, MetadataRequest request, String label, FormTask task)
    {
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>(label)
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                return task.run(transaction, requireForm(transaction, request.objectName));
            }
        });
    }

    private void read(IProject project, MetadataRequest request, FormTask task)
    {
        model(project).getGlobalContext().execute(new AbstractBmTask<Void>("Read 1C form") //$NON-NLS-1$
        {
            @Override
            public Void execute(IBmTransaction transaction, org.eclipse.core.runtime.IProgressMonitor monitor)
            {
                return task.run(transaction, requireForm(transaction, request.objectName));
            }
        });
    }

    /**
     * Resolves the form body of {@code Type.Object.Form.Name} or of a {@code CommonForm.Name}. The body
     * is an external top object, so it is reached through the FQN EDT itself generates for it rather
     * than through the containment reference, which may still hold an unresolved proxy.
     */
    private Form requireForm(IBmTransaction transaction, String formFqn)
    {
        if (formFqn == null || formFqn.isBlank())
        {
            throw new ToolException("Parameter `object_name` is required and must be a form FQN such as" //$NON-NLS-1$
                + " Catalog.Products.Form.ItemForm or CommonForm.Settings."); //$NON-NLS-1$
        }
        var metadata = requireFormMetadata(transaction, formFqn);
        var reference = (EReference)metadata.eClass().getEStructuralFeature("form"); //$NON-NLS-1$
        if (reference == null)
        {
            throw new ToolException("EDT metadata class " + metadata.eClass().getName() + " has no form body."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        var body = transaction.getTopObjectByFqn(fqnGenerator.generateExternalPropertyFqn(metadata, reference));
        if (body instanceof Form)
        {
            return (Form)body;
        }
        if (metadata.getForm() instanceof Form && !metadata.getForm().eIsProxy())
        {
            return (Form)metadata.getForm();
        }
        if (metadata.getFormType() == com._1c.g5.v8.dt.metadata.mdclass.FormType.ORDINARY)
        {
            // An ordinary (8.1) form has a Form.oform body and a different model altogether; these
            // operations only speak the managed-form model.
            throw new ToolException("Form " + formFqn + " is an ordinary (обычная) form, stored as" //$NON-NLS-1$ //$NON-NLS-2$
                + " Form.oform. Only managed (управляемые) forms can be edited here. Ask the user to" //$NON-NLS-1$
                + " convert it, or add a new managed form with createObjectForm.", //$NON-NLS-1$
                ToolErrorType.USER_VISIBLE);
        }
        throw new ToolException("Form " + formFqn + " has no readable Form.form body." //$NON-NLS-1$ //$NON-NLS-2$
            + " Recreate it with createObjectForm."); //$NON-NLS-1$
    }

    private static BasicForm requireFormMetadata(IBmTransaction transaction, String formFqn)
    {
        var direct = transaction.getTopObjectByFqn(formFqn);
        if (direct instanceof BasicForm)
        {
            // CommonForm.<Name> — the form metadata is a top object in its own right.
            return (BasicForm)direct;
        }
        int marker = formFqn.lastIndexOf(".Form."); //$NON-NLS-1$
        if (marker < 0)
        {
            throw new ToolException("`object_name` must be a form FQN: Type.Object.Form.FormName (or" //$NON-NLS-1$
                + " CommonForm.Name). Received: " + formFqn); //$NON-NLS-1$
        }
        var ownerFqn = formFqn.substring(0, marker);
        var formName = formFqn.substring(marker + ".Form.".length()); //$NON-NLS-1$
        var owner = transaction.getTopObjectByFqn(ownerFqn);
        if (!(owner instanceof MdObject))
        {
            throw new ToolException("Metadata object not found: " + ownerFqn); //$NON-NLS-1$
        }
        var forms = ((MdObject)owner).eClass().getEStructuralFeature("forms"); //$NON-NLS-1$
        if (forms == null || !forms.isMany())
        {
            throw new ToolException("Object type " + ((MdObject)owner).eClass().getName() + " has no forms."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        for (var candidate : (List<?>)owner.eGet(forms))
        {
            if (candidate instanceof BasicForm && formName.equals(((BasicForm)candidate).getName()))
            {
                return (BasicForm)candidate;
            }
        }
        throw new ToolException("Form not found: " + formFqn //$NON-NLS-1$
            + ". Create it with createObjectForm, or call inspectObject on " + ownerFqn + " to list its forms."); //$NON-NLS-1$ //$NON-NLS-2$
    }

    // ----------------------------------------------------------------- lookup

    private static FormAttribute findAttribute(Form form, String name)
    {
        for (var attribute : form.getAttributes())
        {
            if (attribute.getName() != null && attribute.getName().equals(name))
            {
                return attribute;
            }
        }
        return null;
    }

    private static FormCommand findCommand(Form form, String name)
    {
        for (var command : form.getFormCommands())
        {
            if (command.getName() != null && command.getName().equals(name))
            {
                return command;
            }
        }
        return null;
    }

    private static FormItem findItem(FormItemContainer container, String name)
    {
        for (var item : container.getItems())
        {
            if (item.getName() != null && item.getName().equals(name))
            {
                return item;
            }
            if (item instanceof FormItemContainer)
            {
                var nested = findItem((FormItemContainer)item, name);
                if (nested != null)
                {
                    return nested;
                }
            }
        }
        return null;
    }

    private static List<FormItem> allItems(FormItemContainer container)
    {
        var result = new ArrayList<FormItem>();
        for (var item : container.getItems())
        {
            result.add(item);
            if (item instanceof FormItemContainer)
            {
                result.addAll(allItems((FormItemContainer)item));
            }
        }
        return result;
    }

    private static FormItemContainer requireContainer(Form form, String parentName)
    {
        if (parentName == null || parentName.isBlank())
        {
            return form;
        }
        var item = findItem(form, parentName);
        if (item == null)
        {
            throw new ToolException("Parent form item not found: " + parentName //$NON-NLS-1$
                + ". Call inspectForm to list the item tree, or omit `parent` to add at the form root."); //$NON-NLS-1$
        }
        if (!(item instanceof FormItemContainer))
        {
            throw new ToolException("Form item `" + parentName + "` is a " + item.eClass().getName() //$NON-NLS-1$ //$NON-NLS-2$
                + " and cannot contain other items. Only groups, pages and tables can."); //$NON-NLS-1$
        }
        return (FormItemContainer)item;
    }

    private static void requireFreeItemName(Form form, String name)
    {
        if (findItem(form, name) != null)
        {
            throw new ToolException("Form item already exists: " + name //$NON-NLS-1$
                + ". Item names are unique across the whole form; pick another one or remove it first."); //$NON-NLS-1$
        }
    }

    // --------------------------------------------------------------- plumbing

    private static AbstractDataPath dataPath(String value)
    {
        var path = FormFactory.eINSTANCE.createDataPath();
        for (var segment : value.trim().replace('/', '.').split("\\.")) //$NON-NLS-1$
        {
            if (!segment.isBlank())
            {
                path.getSegments().add(segment);
            }
        }
        if (path.getSegments().isEmpty())
        {
            throw new ToolException("Parameter `data_path` is empty."); //$NON-NLS-1$
        }
        return path;
    }

    private static String pathOf(AbstractDataPath path)
    {
        return path == null ? null : String.join(".", path.getSegments()); //$NON-NLS-1$
    }

    private FormNewItemDescriptor descriptor(IProject project, MetadataRequest request)
    {
        Map<String, String> title = new LinkedHashMap<>();
        if (request.title != null && !request.title.isBlank())
        {
            title.put(languageCode(project, request), request.title);
        }
        return new FormNewItemDescriptor(request.name, title, false);
    }

    private static int position(MetadataRequest request)
    {
        return request.position != null ? request.position.intValue() : IFormItemManagementService.LAST;
    }

    private void title(org.eclipse.emf.common.util.EMap<String, String> target, IProject project,
        MetadataRequest request, String fallback)
    {
        var value = request.title != null && !request.title.isBlank() ? request.title : fallback;
        if (value != null && !value.isBlank())
        {
            target.put(languageCode(project, request), value);
        }
    }

    private String languageCode(IProject project, MetadataRequest request)
    {
        if (request.languageCode != null && !request.languageCode.isBlank())
        {
            return request.languageCode.trim();
        }
        return MetadataPropertyWriter.DEFAULT_LANGUAGE_CODE;
    }

    private com._1c.g5.v8.dt.platform.version.Version v8Version(IProject project)
    {
        var v8Project = v8ProjectManager.getProject(project);
        if (v8Project == null)
        {
            throw new ToolException("V8 project is not available: " + project.getName()); //$NON-NLS-1$
        }
        return v8Project.getVersion();
    }

    private static com._1c.g5.v8.dt.metadata.mdclass.AdjustableBoolean alwaysAdjustable()
    {
        var value = MdClassFactory.eINSTANCE.createAdjustableBoolean();
        value.setCommon(true);
        return value;
    }

    private static ManagedFormGroupType groupType(String value)
    {
        if (value == null || value.isBlank())
        {
            return ManagedFormGroupType.USUAL_GROUP;
        }
        var normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (var candidate : ManagedFormGroupType.VALUES)
        {
            if (candidate.name().equals(normalized) || candidate.getName().equalsIgnoreCase(value.trim()))
            {
                return candidate;
            }
        }
        throw new ToolException("Invalid `group_type` " + value + ". Valid values: " //$NON-NLS-1$ //$NON-NLS-2$
            + names(ManagedFormGroupType.VALUES) + "."); //$NON-NLS-1$
    }

    private static ManagedFormFieldType fieldType(String value)
    {
        var normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (var candidate : ManagedFormFieldType.VALUES)
        {
            if (candidate.name().equals(normalized) || candidate.getName().equalsIgnoreCase(value.trim()))
            {
                return candidate;
            }
        }
        throw new ToolException("Invalid `item_type` " + value + ". Valid values: " //$NON-NLS-1$ //$NON-NLS-2$
            + names(ManagedFormFieldType.VALUES) + "."); //$NON-NLS-1$
    }

    private static String names(List<? extends org.eclipse.emf.common.util.Enumerator> values)
    {
        var result = new ArrayList<String>();
        for (var value : values)
        {
            result.add(value.getName());
        }
        return String.join(", ", result); //$NON-NLS-1$
    }

    private static String typeNames(TypeDescription description)
    {
        if (description == null || description.getTypes().isEmpty())
        {
            return null;
        }
        var result = new ArrayList<String>();
        for (TypeItem type : description.getTypes())
        {
            result.add(type.getName());
        }
        return String.join(", ", result); //$NON-NLS-1$
    }

    private static String handlerName(FormCommand command)
    {
        var action = command.getAction();
        if (action instanceof com._1c.g5.v8.dt.form.model.FormCommandHandlerContainer)
        {
            var handler = ((com._1c.g5.v8.dt.form.model.FormCommandHandlerContainer)action).getHandler();
            return handler == null ? null : handler.getName();
        }
        return null;
    }

    private static String localized(org.eclipse.emf.common.util.EMap<String, String> value)
    {
        if (value == null || value.isEmpty())
        {
            return null;
        }
        var preferred = value.get(MetadataPropertyWriter.DEFAULT_LANGUAGE_CODE);
        return preferred != null ? preferred : value.get(0).getValue();
    }

    private static String rootCause(Throwable error)
    {
        var current = error;
        while (current.getCause() != null && current.getCause() != current)
        {
            current = current.getCause();
        }
        return current.getClass().getSimpleName()
            + (current.getMessage() == null ? "" : ": " + current.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static void validateIdentifier(String value, String parameter)
    {
        if (value == null || !value.matches("[\\p{L}_][\\p{L}\\p{N}_]*")) //$NON-NLS-1$
        {
            throw new ToolException("Parameter `" + parameter + "` must be a 1C identifier: " + value); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private com._1c.g5.v8.bm.integration.IBmModel model(IProject project)
    {
        var model = modelManager.getModel(project);
        if (model == null)
        {
            throw new ToolException("EDT model is not available for project: " + project.getName()); //$NON-NLS-1$
        }
        return model;
    }
}
