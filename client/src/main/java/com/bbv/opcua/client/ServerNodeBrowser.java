package com.bbv.opcua.client;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by tama on 10/03/2018.
 */
@Controller
public class ServerNodeBrowser {


    ClientExampleRunner runner = new ClientExampleRunner(new BrowseSorterFolderExample());


    @GetMapping("/browse")
    public String greeting(@RequestParam(name = "name", required = false, defaultValue = "World") String name, Model model) {


        try {
            OpcUaClient client = runner.createClient();
            client.connect().get();

            UShort index = client.getNamespaceTable().getIndex("urn:bbv:fischer:color-sorter");
            NodeId nodeId = new NodeId(index, "Sorter");
            model.addAttribute("name", browseNode("", client, nodeId));
        } catch (Exception e) {

            e.printStackTrace();
        }

        return "greeting";
    }

    private String browseNode(String indent, OpcUaClient client, NodeId browseRoot) {
        StringBuilder sb = new StringBuilder();
        try {
            List<Node> nodes = client.getAddressSpace().browse(browseRoot).get();

            for (Node node : nodes) {
                sb.append(String.format("%s Node=%s Type=%s", indent, node.getBrowseName().get().getName(), node.getNodeClass().get().name()));
                sb.append(System.getProperty("line.separator"));
                // recursively browse to children
                browseNode(indent + "  ", client, node.getNodeId().get());
                System.out.println("ciao");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


}
