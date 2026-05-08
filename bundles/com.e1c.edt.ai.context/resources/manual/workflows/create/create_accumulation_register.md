## Safe Workflow: Create AccumulationRegister

```java
IProject project = workspaceRoot.getProject("MyProject");
IV8Project v8project = projectManager.getProject(project);
IBmModel bmModel = modelManager.getModel(project);
IBmGlobalEditingContext globalContext = bmModel.getGlobalContext();

AccumulationRegister register = globalContext.execute(new AbstractBmTask<AccumulationRegister>("Create register") {
    @Override
    public AccumulationRegister execute(IBmTransaction transaction, IProgressMonitor monitor) {
        Configuration configuration = (Configuration)transaction.getTopObjectByFqn("Configuration");

        AccumulationRegister register = mdFactory.createAccumulationRegister();
        register.setName("GoodsInStock");
        register.getSynonym().put("ru", "Goods In Stock");
        register.setRegisterType(AccumulationRegisterType.BALANCE);

        // Add dimension
        AccumulationRegisterDimension warehouse = mdFactory.createAccumulationRegisterDimension();
        warehouse.setName("Warehouse");
        warehouse.getSynonym().put("ru", "Warehouse");

        IEObjectProvider typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem catalogRefType = typeProvider.getProxy(IEObjectTypeNames.CATALOG_REF);
        TypeDescription typeDesc = new TypeDescriptionBuilder()
            .addType(catalogRefType)
            .build();

        warehouse.setType(typeDesc);
        register.getDimensions().add(warehouse);

        // Add resource
        AccumulationRegisterResource quantity = mdFactory.createAccumulationRegisterResource();
        quantity.setName("Quantity");
        quantity.getSynonym().put("ru", "Quantity");

        // Set numeric type for resource
        typeProvider = IEObjectProvider.Registry.INSTANCE
            .get(McorePackage.Literals.TYPE_ITEM, v8project.getVersion());
        TypeItem numberType = (TypeItem)typeProvider.getProxy(IEObjectTypeNames.NUMBER);
        typeDesc = new TypeDescriptionBuilder()
            .addType(numberType)
            .build();

        quantity.setType(typeDesc);
        register.getResources().add(quantity);

        // Set UUIDs manually (RECOMMENDED for JShell)
        register.setUuid(UUID.randomUUID());
        warehouse.setUuid(UUID.randomUUID());
        quantity.setUuid(UUID.randomUUID());

        String fqn = fqnGenerator.generateStandaloneObjectFqn(register.eClass(), register.getName()).toString();
        transaction.attachTopObject((IBmObject)register, fqn);
        configuration.getAccumulationRegisters().add(register);
        return register;
    }
});
```
**Note:** Registers require at least one Dimension. Resources are optional but recommended.
**Note:** `AccumulationRegisterDimension` does not have `setBalance(...)`; do not call it in JShell examples.

### Registrar modes

Use one of these modes deliberately:
- **Mode A: register only for later linking.** Creating only the register is acceptable as an intermediate step, but `GetMarkers` may return SU45 until a document registrar is linked.
- **Mode B: register with registrar document.** Preferred when the user asks for a complete valid register workflow.

Accumulation, accounting, and calculation registers MUST have at least one document that records to them. Registrars are configured on documents, not on registers.

```java
Document goodsReceipt = (Document)transaction.getTopObjectByFqn("Document.GoodsReceipt");
AccumulationRegister stockRegister = (AccumulationRegister)transaction.getTopObjectByFqn("AccumulationRegister.GoodsInStock");
if (goodsReceipt != null && stockRegister != null && !goodsReceipt.getRegisterRecords().contains(stockRegister)) {
    goodsReceipt.getRegisterRecords().add(stockRegister);
}
```

If the registrar document does not exist, create it in the same BM transaction or call `create_document` first, then call `add_document_registers`.

### Expected validation marker for Mode A

If you create the register without linking a registrar document, `GetMarkers` can return SU45: "Некорректный состав регистраторов регистра. Ни один из документов не является регистратором для регистра".

Do not report success while this marker remains unless the user explicitly asked to create an invalid intermediate register for later linking.

### Required post-check

After creating metadata, call `GetMarkers` with `marker_type: "1c"` for the changed file or project. For Mode B, fix all new validation markers before reporting success. For Mode A, explicitly report that registrar linking is still required if SU45 remains.
