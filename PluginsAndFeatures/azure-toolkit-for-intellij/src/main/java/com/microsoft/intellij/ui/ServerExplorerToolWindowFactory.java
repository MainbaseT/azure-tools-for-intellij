/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.ClientProperty;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.hover.TreeHoverListener;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.azure.arcadia.serverexplore.ArcadiaSparkClusterRootModuleImpl;
import com.microsoft.azure.arcadia.serverexplore.ArcadiaSparkComputeNode;
import com.microsoft.azure.arcadia.serverexplore.ArcadiaSparkWorkspaceNode;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkADLAccountNode;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterNode;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterRootModuleImpl;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterNode;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.AzureModuleImpl;
import com.microsoft.tooling.msservices.helpers.collections.ListChangeListener;
import com.microsoft.tooling.msservices.helpers.collections.ListChangedEvent;
import com.microsoft.tooling.msservices.helpers.collections.ObservableList;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;
import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS;
import static com.microsoft.azure.toolkit.intellij.common.component.TreeUtils.INLINE_ACTION_ICON_WIDTH;
import static com.microsoft.azure.toolkit.intellij.common.component.TreeUtils.KEY_SCROLL_PANE;

public class ServerExplorerToolWindowFactory implements ToolWindowFactory, PropertyChangeListener, DumbAware {
    public static final String EXPLORER_WINDOW = "Azure Explorer";

    private final Map<Project, DefaultTreeModel> treeModelMap = new HashMap<>();

    @Override
    @AzureOperation(name = "platform/common.initialize_explorer")
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        // initialize azure service module
        final AzureModule azureModule = new AzureModuleImpl(project);
        //HDInsightUtil.setHDInsightRootModule(azureModule);
        azureModule.setSparkServerlessModule(new CosmosSparkClusterRootModuleImpl(azureModule));
        azureModule.setArcadiaModule(new ArcadiaSparkClusterRootModuleImpl(azureModule));
        // initialize aris service module
        final SqlBigDataClusterModule arisModule = new SqlBigDataClusterModule(project);

        final SortableTreeNode hiddenRoot = new SortableTreeNode();
        final DefaultTreeModel treeModel = new DefaultTreeModel(hiddenRoot);
        final JTree tree = new Tree(treeModel);
        tree.putClientProperty(Action.PLACE, ResourceCommonActionsContributor.AZURE_EXPLORER);

        final var favorRootNode = new com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode<>(AzureExplorer.buildFavoriteRoot(), tree);
        final var acvRootNode = new com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode<>(AzureExplorer.buildAppCentricViewRoot(), tree);
        final var azureRootNode = createTreeNode(azureModule, project);
        final var arisRootNode = createTreeNode(arisModule, project);
        hiddenRoot.add(favorRootNode);
        hiddenRoot.add(acvRootNode);
        hiddenRoot.add(azureRootNode);
        azureModule.load(false); // kick-off asynchronous load of child nodes on all the modules
        hiddenRoot.add(arisRootNode);
        arisModule.load(false);
        treeModelMap.put(project, treeModel);

        // initialize tree
        ClientProperty.put(tree, ANIMATION_IN_RENDERER_ALLOWED, true);
        tree.setRootVisible(false);
        AzureEventBus.on("azure.explorer.highlight_resource", new AzureEventBus.EventListener(e -> TreeUtils.highlightResource(tree, e.getSource())));
        AzureEventBus.on("resource.creation_started.resource", new AzureEventBus.EventListener(e -> {
            if (e.getSource() instanceof AbstractAzResource<?, ?, ?>) {
                TreeUtils.focusResource(tree, (AbstractAzResource<?, ?, ?>) e.getSource());
            }
        }));
        AzureEventBus.on("azure.explorer.focus_resource", new AzureEventBus.EventListener(e -> {
            if (e.getSource() instanceof AbstractAzResource<?,?,?>) {
                TreeUtils.focusResource(tree, (AbstractAzResource<?, ?, ?>) e.getSource());
            }
        }));
        tree.setCellRenderer(new NodeTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        new TreeSpeedSearch(tree);
        final List<? extends com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode<?>> modules = AzureExplorer.getModules()
            .stream()
            .map(m -> new com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode<>(m, tree))
            .toList();
        modules.stream().sorted(Comparator.comparing(treeNode -> treeNode.getLabel())).forEach(azureRootNode::add);
        azureModule.setClearResourcesListener(() -> {
            modules.forEach(m -> m.clearChildren());
            acvRootNode.clearChildren();
        });
        TreeUtils.installSelectionListener(tree);
        TreeUtils.installExpandListener(tree);
        TreeUtils.installMouseListener(tree);
        TreeHoverListener.DEFAULT.addTo(tree);
        treeModel.reload();
        DataManager.registerDataProvider(tree, dataId -> {
            if (StringUtils.equals(dataId, Action.SOURCE)) {
                final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (Objects.nonNull(selectedNode)) {
                    return selectedNode.getUserObject();
                }
            }
            return null;
        });
        // add a click handler for the tree
        tree.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                treeMousePressed(e, tree);
            }
        });
        // add the tree to the window
        final JBScrollPane scrollPane = new JBScrollPane(tree);
        tree.putClientProperty(KEY_SCROLL_PANE, scrollPane);
        toolWindow.getComponent().add(scrollPane);

        // set tree and tree path to expand the node later
        azureModule.setTree(tree);
        azureModule.setTreePath(tree.getPathForRow(0));

        // setup toolbar icons
        addToolbarItems(toolWindow, project, azureModule);
        tree.getModel().addTreeModelListener(new TreeUtils.FocusResourceListener(tree));
    }

    private void treeMousePressed(MouseEvent e, JTree tree) {
        // delegate click to the node's click action if this is a left button click
        if (SwingUtilities.isLeftMouseButton(e)) {
            final TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
            if (treePath == null) {
                return;
            }
            // get the tree node associated with left mouse click
            final Node node = getTreeNodeOnMouseClick(tree, treePath);
            // if the node in question is in a "loading" state then we
            // do not propagate the click event to it
            if (Objects.nonNull(node) && !node.isLoading()) {
                node.getClickAction().fireNodeActionEvent();
            }
            // for right click show the context menu populated with all the
            // actions from the node
        } else if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
            final TreePath treePath = tree.getClosestPathForLocation(e.getX(), e.getY());
            if (treePath == null) {
                return;
            }
            // get the tree node associated with right mouse click
            final Node node = getTreeNodeOnMouseClick(tree, treePath);
            if (Objects.nonNull(node) && node.hasNodeActions()) {
                // select the node which was right-clicked
                tree.getSelectionModel().setSelectionPath(treePath);

                final JPopupMenu menu = createPopupMenuForNode(node);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    @Nullable
    private Node getTreeNodeOnMouseClick(JTree tree, TreePath treePath) {
        final Object raw = treePath.getLastPathComponent();
        if (raw instanceof com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode || raw instanceof LoadingNode ||
            raw instanceof com.microsoft.azure.toolkit.intellij.common.component.Tree.LoadMoreNode) {
            return null;
        }
        final SortableTreeNode treeNode = (SortableTreeNode) raw;
        final Node node = (Node) treeNode.getUserObject();
        // set tree and tree path to expand the node later
        node.setTree(tree);
        node.setTreePath(treePath);
        return node;
    }

    private JPopupMenu createPopupMenuForNode(Node node) {
        final JPopupMenu menu = new JPopupMenu();
        final LinkedHashMap<Integer, List<NodeAction>> sortedNodeActionsGroupMap =
            node.getNodeActions().stream()
                .sorted(Comparator.comparing(NodeAction::getGroup).thenComparing(NodeAction::getPriority).thenComparing(NodeAction::getName))
                .collect(Collectors.groupingBy(NodeAction::getGroup, LinkedHashMap::new, Collectors.toList()));
        // Convert node actions map to menu items, as linked hash map keeps ordered, no need to sort again
        sortedNodeActionsGroupMap.forEach((groupNumber, actions) -> {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            actions.stream().map(this::createMenuItemFromNodeAction).forEachOrdered(menu::add);
        });
        return menu;
    }

    private JMenuItem createMenuItemFromNodeAction(NodeAction nodeAction) {
        final JMenuItem menuItem = new JMenuItem(nodeAction.getName());
        menuItem.setEnabled(nodeAction.isEnabled());
        final AzureIcon iconSymbol = nodeAction.getIconSymbol();
        if (Objects.nonNull(iconSymbol)) {
            menuItem.setIcon(IntelliJAzureIcons.getIcon(iconSymbol));
        } else if (StringUtils.isNotBlank(nodeAction.getIconPath())) {
            menuItem.setIcon(UIHelperImpl.loadIcon(nodeAction.getIconPath()));
        }
        // delegate the menu item click to the node action's listeners
        menuItem.addActionListener(e -> nodeAction.fireNodeActionEvent());
        return menuItem;
    }

    private SortableTreeNode createTreeNode(Node node, Project project) {
        final SortableTreeNode treeNode = new SortableTreeNode(node, true);

        // associate the DefaultMutableTreeNode with the Node via it's "viewData"
        // property; this allows us to quickly retrieve the DefaultMutableTreeNode
        // object associated with a Node
        node.setViewData(treeNode);

        // listen for property change events on the node
        node.addPropertyChangeListener(this);

        // listen for structure changes on the node, i.e. when child nodes are
        // added or removed
        node.getChildNodes().addChangeListener(new NodeListChangeListener(treeNode, project));

        // create child tree nodes for each child node
        node.getChildNodes().stream()
            .filter(s -> !isOutdatedModule(s))
            .sorted(Comparator.comparing(Node::getPriority).thenComparing(Node::getName))
            .map(childNode -> createTreeNode(childNode, project))
            .forEach(treeNode::add);

        return treeNode;
    }

    private void removeEventHandlers(Node node) {
        node.removePropertyChangeListener(this);

        final ObservableList<Node> childNodes = node.getChildNodes();
        childNodes.removeAllChangeListeners();

        if (node.hasChildNodes()) {
            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire which should recursively
            // clean up event handlers on it's children
            node.removeAllChildNodes();
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        // if we are not running on the dispatch thread then switch
        // to dispatch thread
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            AzureTaskManager.getInstance().runAndWait(() -> propertyChange(evt), AzureTask.Modality.ANY);
            return;
        }

        // this event is fired whenever a property on a node in the
        // model changes; we respond by triggering a node change
        // event in the tree's model
        final Node node = (Node) evt.getSource();

        // the treeModel object can be null before it is initialized
        // from createToolWindowContent; we ignore property change
        // notifications till we have a valid model object
        final DefaultTreeModel treeModel = treeModelMap.get((Project) node.getProject());
        if (treeModel != null) {
            synchronized (treeModel) {
                treeModel.nodeChanged((TreeNode) node.getViewData());
            }
        }
    }

    private class NodeListChangeListener implements ListChangeListener {
        private final SortableTreeNode treeNode;
        private final Project project;

        NodeListChangeListener(SortableTreeNode treeNode, Project project) {
            this.treeNode = treeNode;
            this.project = project;
        }

        @Override
        public void listChanged(final ListChangedEvent e) {
            // if we are not running on the dispatch thread then switch
            // to dispatch thread
            if (!ApplicationManager.getApplication().isDispatchThread()) {
                AzureTaskManager.getInstance().runAndWait(() -> listChanged(e), AzureTask.Modality.ANY);
                return;
            }

            switch (e.getAction()) {
                case add:
                    // create child tree nodes for the new nodes
                    for (final Node childNode : (Collection<Node>) e.getNewItems()) {
                        if (isOutdatedModule(childNode)) {
                            continue;
                        }
                        treeNode.add(createTreeNode(childNode, project));
                    }
                    break;
                case remove:
                    // unregistered all event handlers recursively and remove
                    // child nodes from the tree
                    for (final Node childNode : (Collection<Node>) e.getOldItems()) {
                        if (isOutdatedModule(childNode)) {
                            continue;
                        }
                        // remove this node from the tree
                        removeEventHandlers(childNode);
                        treeNode.remove((MutableTreeNode) childNode.getViewData());
                    }
                    break;
                default:
                    break;
            }
            final DefaultTreeModel model = treeModelMap.get(project);
            if (model != null) {
                synchronized (model) {
                    model.reload(treeNode);
                }
            }
        }
    }

    private static class NodeTreeCellRenderer extends NodeRenderer {
        private final List<Icon> inlineActionIcons = new ArrayList<>();
        private Rectangle viewportRect;
        private int scrolledWidth;

        @Override
        public void customizeCellRenderer(@NotNull JTree jtree,
                                          final Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean isLeaf,
                                          int row,
                                          boolean focused) {
            inlineActionIcons.clear();
            if (value instanceof com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode) {
                final com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode<?> node =
                    (com.microsoft.azure.toolkit.intellij.common.component.Tree.TreeNode<?>) value;
                final int hoveredRow = TreeHoverListener.getHoveredRow(jtree);
                inlineActionIcons.addAll(node.getInlineActionViews().stream()
                    .map(av -> IntelliJAzureIcons.getIcon(av.getIconPath()))
                    // TODO @miller should not check the value of inlineActionIcon
                    .filter(icon -> hoveredRow == row || icon == AllIcons.Nodes.Favorite).toList());
                final JBScrollPane scrollPane = (JBScrollPane) jtree.getClientProperty(KEY_SCROLL_PANE);
                if (Objects.nonNull(scrollPane)) {
                    this.viewportRect = scrollPane.getViewport().getViewRect();
                }
                TreeUtils.renderMyTreeNode(jtree, node, selected, this);
                return;
            } else if (value instanceof com.microsoft.azure.toolkit.intellij.common.component.Tree.LoadMoreNode) {
                TreeUtils.renderLoadModeNode(jtree, (com.microsoft.azure.toolkit.intellij.common.component.Tree.LoadMoreNode) value, selected, this);
                return;
            } else if (value instanceof LoadingNode) {
                super.customizeCellRenderer(jtree, value, selected, expanded, isLeaf, row, focused);
                return;
            }
            super.customizeCellRenderer(jtree, value, selected, expanded, isLeaf, row, focused);

            // if the node has an icon set then we use that
            final SortableTreeNode treeNode = (SortableTreeNode) value;
            final Node node = (Node) treeNode.getUserObject();

            // "node" can be null if it's the root node which we keep hidden to simulate
            // a multi-root tree control
            if (node == null) {
                return;
            }

            final Icon icon = node.getIcon();
            final String iconPath = node.getIconPath();
            final AzureIcon iconSymbol = node.getIconSymbol();
            if (Objects.nonNull(icon)) {
                setIcon(icon);
            } else if (Objects.nonNull(iconSymbol)) {
                setIcon(IntelliJAzureIcons.getIcon(iconSymbol));
            } else if (StringUtils.isNotBlank(iconPath)) {
                setIcon(UIHelperImpl.loadIcon(iconPath));
            }

            // setup a tooltip
            setToolTipText(node.getToolTip());
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            // WARNING: `this.getX()` is `0` when painting hovered floating overflow node.
            if (this.getX() > 0) {
                paintInlineActionIcons(g);
            }
        }

        private void paintInlineActionIcons(Graphics g) {
            // WARNING: `this.getX()` is `0` when painting hovered floating overflow node.
            int prevIconBoxOffset = this.viewportRect.x + this.viewportRect.width - this.getX() - TreeUtils.NODE_PADDING;
            for (final Icon icon : inlineActionIcons) {
                final int iconBoxWidth = INLINE_ACTION_ICON_WIDTH + TreeUtils.INLINE_ACTION_ICON_MARGIN;
                final int iconBoxOffset = prevIconBoxOffset - iconBoxWidth;
                if (iconBoxOffset > 0) {
                    final int iconOffset = iconBoxOffset + TreeUtils.INLINE_ACTION_ICON_MARGIN / 2;
                    g.setColor(getBackground());
                    g.fillRect(iconBoxOffset, 0, iconBoxWidth, getHeight());
                    paintIcon(g, icon, iconOffset);
                    prevIconBoxOffset = iconBoxOffset;
                }
            }
        }
    }

    private void addToolbarItems(ToolWindow toolWindow, final Project project, final AzureModule azureModule) {
        final AnAction refreshAction = new RefreshAllAction(azureModule);
        final AnAction feedbackAction = ActionManager.getInstance().getAction("Actions.ProvideFeedback");
        final AnAction getStartAction = ActionManager.getInstance().getAction("Actions.GettingStart");
        final AnAction signInAction = ActionManager.getInstance().getAction("AzureToolkit.AzureSignIn");
        final AnAction selectSubscriptionsAction = ActionManager.getInstance().getAction("AzureToolkit.SelectSubscriptions");
        toolWindow.setTitleActions(Arrays.asList(getStartAction, refreshAction, selectSubscriptionsAction, signInAction, Separator.create(), feedbackAction));
        if (toolWindow instanceof ToolWindowEx) {
            final AnAction devBlogsAction = ActionManager.getInstance().getAction("AzureToolkit.ViewDevBlogs");
            final AnAction documentAction = ActionManager.getInstance().getAction("AzureToolkit.ViewToolingDocument");
            final AnAction whatsNewAction = ActionManager.getInstance().getAction("Actions.WhatsNew");
            final AnAction reportIssueAction = ActionManager.getInstance().getAction("AzureToolkit.GithubIssue");
            final AnAction featureRequestAction = ActionManager.getInstance().getAction("AzureToolkit.FeatureRequest");
            final AnAction openSdkReferenceBookAction = ActionManager.getInstance().getAction("user/AzureToolkit.OpenSdkReferenceBook");
            final AnAction openResourceConnectionExplorerAction = ActionManager.getInstance().getAction("AzureToolkit.OpenResourceConnectionExplorerAction");
            final AnAction openAzureSettingsAction = ActionManager.getInstance().getAction(OPEN_AZURE_SETTINGS.getId());
            (toolWindow).setAdditionalGearActions(new DefaultActionGroup(openSdkReferenceBookAction, openAzureSettingsAction, openResourceConnectionExplorerAction,
                Separator.create(), reportIssueAction, featureRequestAction, feedbackAction, Separator.create(), devBlogsAction, documentAction));
        }
    }

    private boolean isOutdatedModule(Node node) {
        // TODO: After SOC, Synapse and Big data SQL Server build nodes in a new way, replace the code with the commented part
        //return !(node instanceof AzureModule || node instanceof ArcadiaSparkClusterRootModuleImpl || node instanceof CosmosSparkClusterRootModuleImpl);
        return !(node instanceof AzureModule
                || node instanceof ArcadiaSparkClusterRootModuleImpl
                || node instanceof ArcadiaSparkComputeNode
                || node instanceof ArcadiaSparkWorkspaceNode
                || node instanceof CosmosSparkClusterRootModuleImpl
                || node instanceof CosmosSparkADLAccountNode
                || node instanceof CosmosSparkClusterNode
                || node instanceof SqlBigDataClusterModule
                || node instanceof SqlBigDataClusterNode
        );
    }

    private static class RefreshAllAction extends AnAction implements DumbAware {
        private final AzureModule azureModule;

        public RefreshAllAction(AzureModule azureModule) {
            super("Refresh All", "Refresh Azure nodes list", AllIcons.Actions.Refresh);
            this.azureModule = azureModule;
        }

        @Override
        public void actionPerformed(AnActionEvent event) {
            azureModule.load(true);
            AzureExplorer.refreshAll();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            final boolean isSignIn = IdeAzureAccount.getInstance().isLoggedIn();
            e.getPresentation().setEnabled(isSignIn);
        }

        @Override
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }
}
