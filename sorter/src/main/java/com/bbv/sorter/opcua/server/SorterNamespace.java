/*
 * Copyright (c) 2016 Kevin Herron
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.html.
 */

package com.bbv.sorter.opcua.server;

import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.methods.SqrtMethod;
import com.google.common.collect.Lists;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRank;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.*;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.OpcUaBinaryDataTypeDictionary;
import org.eclipse.milo.opcua.stack.core.types.OpcUaDataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bbv.sorter.opcua.server.types.CustomDataType;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.*;

public class SorterNamespace implements Namespace {

    public static final String NAMESPACE_URI = "urn:bbv:fischer:color-sorter";

    private static final Object[][] STATIC_SCALAR_NODES = new Object[][]{
            {"Boolean", Identifiers.Boolean, new Variant(false)},
            {"Byte", Identifiers.Byte, new Variant(ubyte(0x00))},
            {"SByte", Identifiers.SByte, new Variant((byte) 0x00)},
            {"Int16", Identifiers.Int16, new Variant((short) 16)},
            {"Int32", Identifiers.Int32, new Variant(32)},
            {"Int64", Identifiers.Int64, new Variant(64L)},
            {"UInt16", Identifiers.UInt16, new Variant(ushort(16))},
            {"UInt32", Identifiers.UInt32, new Variant(uint(32))},
            {"UInt64", Identifiers.UInt64, new Variant(ulong(64L))},
            {"Float", Identifiers.Float, new Variant(3.14f)},
            {"Double", Identifiers.Double, new Variant(3.14d)},
            {"String", Identifiers.String, new Variant("string value")},
            {"DateTime", Identifiers.DateTime, new Variant(DateTime.now())},
            {"Guid", Identifiers.Guid, new Variant(UUID.randomUUID())},
            {"ByteString", Identifiers.ByteString, new Variant(new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04}))},
            {"XmlElement", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>"))},
            {"LocalizedText", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text"))},
            {"QualifiedName", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg"))},
            {"NodeId", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd"))},

            {"Duration", Identifiers.Duration, new Variant(1.0)},
            {"UtcTime", Identifiers.UtcTime, new Variant(DateTime.now())},
    };

    private static final Object[][] STATIC_ARRAY_NODES = new Object[][]{
            {"BooleanArray", Identifiers.Boolean, false},
            {"ByteArray", Identifiers.Byte, ubyte(0)},
            {"SByteArray", Identifiers.SByte, (byte) 0x00},
            {"Int16Array", Identifiers.Int16, (short) 16},
            {"Int32Array", Identifiers.Int32, 32},
            {"Int64Array", Identifiers.Int64, 64L},
            {"UInt16Array", Identifiers.UInt16, ushort(16)},
            {"UInt32Array", Identifiers.UInt32, uint(32)},
            {"UInt64Array", Identifiers.UInt64, ulong(64L)},
            {"FloatArray", Identifiers.Float, 3.14f},
            {"DoubleArray", Identifiers.Double, 3.14d},
            {"StringArray", Identifiers.String, "string value"},
            {"DateTimeArray", Identifiers.DateTime, new Variant(DateTime.now())},
            {"GuidArray", Identifiers.Guid, new Variant(UUID.randomUUID())},
            {"ByteStringArray", Identifiers.ByteString, new Variant(new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04}))},
            {"XmlElementArray", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>"))},
            {"LocalizedTextArray", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text"))},
            {"QualifiedNameArray", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg"))},
            {"NodeIdArray", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd"))}
    };


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Random random = new Random();

    private final SubscriptionModel subscriptionModel;

    private final NodeFactory nodeFactory;

    private final OpcUaServer server;
    private final UShort namespaceIndex;

    public SorterNamespace(OpcUaServer server, UShort namespaceIndex) {
        this.server = server;
        this.namespaceIndex = namespaceIndex;

        subscriptionModel = new SubscriptionModel(server, this);

        nodeFactory = new NodeFactory(
                server.getNodeMap(),
                server.getObjectTypeManager(),
                server.getVariableTypeManager()
        );

        try {
            UaFolderNode sorterFolder = createSorterFolder(server, namespaceIndex);
            addConveyorNodes(sorterFolder);


            addCustomDataTypeVariable(sorterFolder);

            addCustomObjectTypeAndInstance(sorterFolder);
        } catch (UaException e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
    }

    private void addConveyorNodes(UaFolderNode sorterFolder) {
        UaFolderNode conveyorFolder = createConveyorNode(sorterFolder);
        addConveyorVariableNodes(conveyorFolder);
        addMethodNode(conveyorFolder);
        addDataAccessNodes(sorterFolder);
    }

    private UaFolderNode createConveyorNode(UaFolderNode sorterNode) {
        UaFolderNode conveyorFolder = new UaFolderNode(
                server.getNodeMap(),
                new NodeId(namespaceIndex, "Sorter/Conveyor"),
                new QualifiedName(namespaceIndex, "Conveyor"),
                LocalizedText.english("Conveyor")
        );

        server.getNodeMap().addNode(conveyorFolder);
        sorterNode.addOrganizes(conveyorFolder);
        return conveyorFolder;
    }

    private UaFolderNode createSorterFolder(OpcUaServer server, UShort namespaceIndex) throws UaException {
        // Create a "Sorter" folder and add it to the node manager
        NodeId folderNodeId = new NodeId(namespaceIndex, "Sorter");

        UaFolderNode folderNode = new UaFolderNode(
                server.getNodeMap(),
                folderNodeId,
                new QualifiedName(namespaceIndex, "Sorter"),
                LocalizedText.english("Sorter")
        );

        server.getNodeMap().addNode(folderNode);

        // Make sure our new folder shows up under the server's Objects folder
        server.getUaNamespace().addReference(
                Identifiers.ObjectsFolder,
                Identifiers.Organizes,
                true,
                folderNodeId.expanded(),
                NodeClass.Object
        );
        return folderNode;
    }

    @Override
    public UShort getNamespaceIndex() {
        return namespaceIndex;
    }

    @Override
    public String getNamespaceUri() {
        return NAMESPACE_URI;
    }


    private void addConveyorVariableNodes(UaFolderNode conveyorFolder) {


        // Mode
        {
            String name = "Mode";
            NodeId typeId = Identifiers.Boolean;
            //NodeId typeId = Identifiers.StateVariableType;
            AttributeDelegate modeAttributeDelegate = getModeAttributeDelegate();
            double minimumSamplingInterval = 1.0;
            Variant variant = new Variant(false);

            UaVariableNode conveyorModeNode = createConveyorVariableNode(name, typeId, modeAttributeDelegate, minimumSamplingInterval, variant);

            server.getNodeMap().addNode(conveyorModeNode);
            conveyorFolder.addOrganizes(conveyorModeNode);


        }

        // Mode
        {
            String name = "Status";
            NodeId typeId = Identifiers.Boolean;
            //NodeId typeId = Identifiers.StateVariableType;
            AttributeDelegate statusAttributeDelegate = getStatusAttributeDelegate();
            double minimumSamplingInterval = 1.0;
            Variant variant = new Variant(false);

            UaVariableNode conveyorStatusNode = createConveyorVariableNode(name, typeId, statusAttributeDelegate, minimumSamplingInterval, variant);

            server.getNodeMap().addNode(conveyorStatusNode);
            conveyorFolder.addOrganizes(conveyorStatusNode);


        }


    }

    private UaVariableNode createConveyorVariableNode(String name, NodeId typeId, AttributeDelegate modeAttributeDelegate, double minimumSamplingInterval, Variant variant) {
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/Conveyor/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        node.setMinimumSamplingInterval(minimumSamplingInterval);

        node.setValue(new DataValue(variant));


        node.setAttributeDelegate(AttributeDelegateChain.create(
                modeAttributeDelegate,
                ValueLoggingDelegate::new
        ));
        return node;
    }

    private AttributeDelegate getModeAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                return new DataValue(new Variant(ConveyorFactory.createConveyor().getMode()));
            }
        };
    }

    private AttributeDelegate getStatusAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                return new DataValue(new Variant(ConveyorFactory.createConveyor().getStatus()));
            }
        };
    }


    private void addDataAccessNodes(UaFolderNode rootNode) {
        // DataAccess folder
        UaFolderNode dataAccessFolder = new UaFolderNode(
                server.getNodeMap(),
                new NodeId(namespaceIndex, "Sorter/DataAccess"),
                new QualifiedName(namespaceIndex, "DataAccess"),
                LocalizedText.english("DataAccess")
        );

        server.getNodeMap().addNode(dataAccessFolder);
        rootNode.addOrganizes(dataAccessFolder);

        // AnalogItemType node
        AnalogItemNode node = nodeFactory.createVariable(
                new NodeId(namespaceIndex, "Sorter/DataAccess/AnalogValue"),
                new QualifiedName(namespaceIndex, "AnalogValue"),
                LocalizedText.english("AnalogValue"),
                Identifiers.AnalogItemType,
                AnalogItemNode.class
        );

        node.setDataType(Identifiers.Double);
        node.setValue(new DataValue(new Variant(3.14d)));

        node.setEURange(new Range(0.0, 100.0));

        server.getNodeMap().addNode(node);
        dataAccessFolder.addOrganizes(node);
    }

    private void addMethodNode(UaFolderNode folderNode) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/sqrt(x)"))
                .setBrowseName(new QualifiedName(namespaceIndex, "sqrt(x)"))
                .setDisplayName(new LocalizedText(null, "sqrt(x)"))
                .setDescription(
                        LocalizedText.english("Returns the correctly rounded positive square root of a double value."))
                .build();


        try {
            AnnotationBasedInvocationHandler invocationHandler =
                    AnnotationBasedInvocationHandler.fromAnnotatedObject(
                            server.getNodeMap(), new SqrtMethod());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);

            folderNode.addReference(new Reference(
                    folderNode.getNodeId(),
                    Identifiers.HasComponent,
                    methodNode.getNodeId().expanded(),
                    methodNode.getNodeClass(),
                    true
            ));

            methodNode.addReference(new Reference(
                    methodNode.getNodeId(),
                    Identifiers.HasComponent,
                    folderNode.getNodeId().expanded(),
                    folderNode.getNodeClass(),
                    false
            ));
        } catch (Exception e) {
            logger.error("Error creating sqrt() method.", e);
        }
    }

    private void addCustomObjectTypeAndInstance(UaFolderNode rootFolder) throws UaException {
        // Define a new ObjectType called "MyObjectType".
        UaObjectTypeNode objectTypeNode = UaObjectTypeNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/MyObjectType"))
                .setBrowseName(new QualifiedName(namespaceIndex, "MyObjectType"))
                .setDisplayName(LocalizedText.english("MyObjectType"))
                .setIsAbstract(false)
                .build();

        // "Foo" and "Bar" are members. These nodes are what are called "instance declarations" by the spec.
        UaVariableNode foo = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/MyObjectType.Foo"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "Foo"))
                .setDisplayName(LocalizedText.english("Foo"))
                .setDataType(Identifiers.Int16)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        foo.setValue(new DataValue(new Variant(0)));
        objectTypeNode.addComponent(foo);

        UaVariableNode bar = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/MyObjectType.Bar"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "Bar"))
                .setDisplayName(LocalizedText.english("Bar"))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        bar.setValue(new DataValue(new Variant("bar")));
        objectTypeNode.addComponent(bar);

        // Tell the ObjectTypeManager about our new type.
        // This let's us use NodeFactory to instantiate instances of the type.
        server.getObjectTypeManager().registerObjectType(
                objectTypeNode.getNodeId(),
                UaObjectNode.class,
                UaObjectNode::new
        );

        // Add our ObjectTypeNode as a subtype of BaseObjectType.
        server.getUaNamespace().addReference(
                Identifiers.BaseObjectType,
                Identifiers.HasSubtype,
                true,
                objectTypeNode.getNodeId().expanded(),
                NodeClass.ObjectType
        );

        // Add the inverse SubtypeOf relationship.
        objectTypeNode.addReference(new Reference(
                objectTypeNode.getNodeId(),
                Identifiers.HasSubtype,
                Identifiers.BaseObjectType.expanded(),
                NodeClass.ObjectType,
                false
        ));

        // Add it into the address space.
        server.getNodeMap().addNode(objectTypeNode);

        // Use NodeFactory to create instance of MyObjectType called "MyObject".
        // NodeFactory takes care of recursively instantiating MyObject member nodes
        // as well as adding all nodes to the address space.
        UaObjectNode myObject = nodeFactory.createObject(
                new NodeId(namespaceIndex, "Sorter/MyObject"),
                new QualifiedName(namespaceIndex, "MyObject"),
                LocalizedText.english("MyObject"),
                objectTypeNode.getNodeId()
        );

        // Add forward and inverse references from the root folder.
        rootFolder.addOrganizes(myObject);

        myObject.addReference(new Reference(
                myObject.getNodeId(),
                Identifiers.Organizes,
                rootFolder.getNodeId().expanded(),
                rootFolder.getNodeClass(),
                false
        ));
    }

    private void addCustomDataTypeVariable(UaFolderNode rootFolder) {
        // add a custom DataTypeNode as a subtype of the built-in Structure DataTypeNode
        NodeId dataTypeId = new NodeId(namespaceIndex, "DataType.CustomDataType");

        UaDataTypeNode dataTypeNode = new UaDataTypeNode(
                server.getNodeMap(),
                dataTypeId,
                new QualifiedName(namespaceIndex, "CustomDataType"),
                LocalizedText.english("CustomDataType"),
                LocalizedText.english("CustomDataType"),
                uint(0),
                uint(0),
                false
        );

        // Inverse ref to Structure
        dataTypeNode.addReference(new Reference(
                dataTypeId,
                Identifiers.HasSubtype,
                Identifiers.Structure.expanded(),
                NodeClass.DataType,
                false
        ));

        // Forward ref from Structure
        Optional<UaDataTypeNode> structureDataTypeNode = server.getNodeMap()
                .getNode(Identifiers.Structure)
                .map(UaDataTypeNode.class::cast);

        structureDataTypeNode.ifPresent(node ->
                node.addReference(new Reference(
                        node.getNodeId(),
                        Identifiers.HasSubtype,
                        dataTypeId.expanded(),
                        NodeClass.DataType,
                        true
                ))
        );

        // Create a dictionary, binaryEncodingId, and register the codec under that id
        OpcUaBinaryDataTypeDictionary dictionary = new OpcUaBinaryDataTypeDictionary(
                "urn:eclipse:milo:example:custom-data-type"
        );

        NodeId binaryEncodingId = new NodeId(namespaceIndex, "DataType.CustomDataType.BinaryEncoding");

        dictionary.registerStructCodec(
                new CustomDataType.Codec().asBinaryCodec(),
                "CustomDataType",
                binaryEncodingId
        );

        // Register dictionary with the shared DataTypeManager instance
        OpcUaDataTypeManager.getInstance().registerTypeDictionary(dictionary);


        UaVariableNode customDataTypeVariable = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/CustomDataTypeVariable"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "CustomDataTypeVariable"))
                .setDisplayName(LocalizedText.english("CustomDataTypeVariable"))
                .setDataType(dataTypeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        CustomDataType value = new CustomDataType(
                "foo",
                uint(42),
                true
        );

        ExtensionObject xo = ExtensionObject.encode(value, binaryEncodingId);

        customDataTypeVariable.setValue(new DataValue(new Variant(xo)));

        rootFolder.addOrganizes(customDataTypeVariable);

        customDataTypeVariable.addReference(new Reference(
                customDataTypeVariable.getNodeId(),
                Identifiers.Organizes,
                rootFolder.getNodeId().expanded(),
                rootFolder.getNodeClass(),
                false
        ));
    }

    private void addArrayNodes(UaFolderNode rootNode) {
        UaFolderNode arrayTypesFolder = new UaFolderNode(
                server.getNodeMap(),
                new NodeId(namespaceIndex, "Sorter/ArrayTypes"),
                new QualifiedName(namespaceIndex, "ArrayTypes"),
                LocalizedText.english("ArrayTypes")
        );

        server.getNodeMap().addNode(arrayTypesFolder);
        rootNode.addOrganizes(arrayTypesFolder);

        for (Object[] os : STATIC_ARRAY_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            Object value = os[2];
            Object array = Array.newInstance(value.getClass(), 4);
            for (int i = 0; i < 4; i++) {
                Array.set(array, i, value);
            }
            Variant variant = new Variant(array);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                    .setNodeId(new NodeId(namespaceIndex, "Sorter/ArrayTypes/" + name))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(new QualifiedName(namespaceIndex, name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .setValueRank(ValueRank.OneDimension.getValue())
                    .setArrayDimensions(new UInteger[]{uint(0)})
                    .build();

            node.setValue(new DataValue(variant));

            node.setAttributeDelegate(new ValueLoggingDelegate());

            server.getNodeMap().addNode(node);
            arrayTypesFolder.addOrganizes(node);
        }
    }

    private void addScalarNodes(UaFolderNode rootNode) {
        UaFolderNode scalarTypesFolder = new UaFolderNode(
                server.getNodeMap(),
                new NodeId(namespaceIndex, "Sorter/ScalarTypes"),
                new QualifiedName(namespaceIndex, "ScalarTypes"),
                LocalizedText.english("ScalarTypes")
        );

        server.getNodeMap().addNode(scalarTypesFolder);
        rootNode.addOrganizes(scalarTypesFolder);

        for (Object[] os : STATIC_SCALAR_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            Variant variant = (Variant) os[2];

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                    .setNodeId(new NodeId(namespaceIndex, "Sorter/ScalarTypes/" + name))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(new QualifiedName(namespaceIndex, name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(variant));

            node.setAttributeDelegate(new ValueLoggingDelegate());

            server.getNodeMap().addNode(node);
            scalarTypesFolder.addOrganizes(node);
        }
    }

    private void addAdminReadableNodes(UaFolderNode rootNode) {
        UaFolderNode adminFolder = new UaFolderNode(
                server.getNodeMap(),
                new NodeId(namespaceIndex, "Sorter/OnlyAdminCanRead"),
                new QualifiedName(namespaceIndex, "OnlyAdminCanRead"),
                LocalizedText.english("OnlyAdminCanRead")
        );

        server.getNodeMap().addNode(adminFolder);
        rootNode.addOrganizes(adminFolder);

        String name = "String";
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/OnlyAdminCanRead/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant("shh... don't tell the lusers")));

        node.setAttributeDelegate(new RestrictedAccessDelegate(identity -> {
            if ("admin".equals(identity)) {
                return AccessLevel.READ_WRITE;
            } else {
                return AccessLevel.NONE;
            }
        }));

        server.getNodeMap().addNode(node);
        adminFolder.addOrganizes(node);
    }

    private void addAdminWritableNodes(UaFolderNode rootNode) {
        UaFolderNode adminFolder = new UaFolderNode(
                server.getNodeMap(),
                new NodeId(namespaceIndex, "Sorter/OnlyAdminCanWrite"),
                new QualifiedName(namespaceIndex, "OnlyAdminCanWrite"),
                LocalizedText.english("OnlyAdminCanWrite")
        );

        server.getNodeMap().addNode(adminFolder);
        rootNode.addOrganizes(adminFolder);

        String name = "String";
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/OnlyAdminCanWrite/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(new Variant("admin was here")));

        node.setAttributeDelegate(new RestrictedAccessDelegate(identity -> {
            if ("admin".equals(identity)) {
                return AccessLevel.READ_WRITE;
            } else {
                return AccessLevel.READ_ONLY;
            }
        }));

        server.getNodeMap().addNode(node);
        adminFolder.addOrganizes(node);
    }

    @Override
    public CompletableFuture<List<Reference>> browse(AccessContext context, NodeId nodeId) {
        ServerNode node = server.getNodeMap().get(nodeId);

        if (node != null) {
            return CompletableFuture.completedFuture(node.getReferences());
        } else {
            return FutureUtils.failedFuture(new UaException(StatusCodes.Bad_NodeIdUnknown));
        }
    }

    @Override
    public void read(
            ReadContext context,
            Double maxAge,
            TimestampsToReturn timestamps,
            List<ReadValueId> readValueIds) {

        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId readValueId : readValueIds) {
            ServerNode node = server.getNodeMap().get(readValueId.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(
                        new AttributeContext(context),
                        readValueId.getAttributeId(),
                        timestamps,
                        readValueId.getIndexRange(),
                        readValueId.getDataEncoding()
                );

                results.add(value);
            } else {
                results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            ServerNode node = server.getNodeMap().get(writeValue.getNodeId());

            if (node != null) {
                try {
                    node.writeAttribute(
                            new AttributeContext(context),
                            writeValue.getAttributeId(),
                            writeValue.getValue(),
                            writeValue.getIndexRange()
                    );

                    results.add(StatusCode.GOOD);

                    logger.info(
                            "Wrote value {} to {} attribute of {}",
                            writeValue.getValue().getValue(),
                            AttributeId.from(writeValue.getAttributeId()).map(Object::toString).orElse("unknown"),
                            node.getNodeId());
                } catch (UaException e) {
                    logger.error("Unable to write value={}", writeValue.getValue(), e);
                    results.add(e.getStatusCode());
                }
            } else {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public Optional<MethodInvocationHandler> getInvocationHandler(NodeId methodId) {
        Optional<ServerNode> node = server.getNodeMap().getNode(methodId);

        return node.flatMap(n -> {
            if (n instanceof UaMethodNode) {
                return ((UaMethodNode) n).getInvocationHandler();
            } else {
                return Optional.empty();
            }
        });
    }

}
