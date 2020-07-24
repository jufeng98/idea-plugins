package org.javamaster.idea.redis;

import static java.awt.event.MouseEvent.BUTTON2;
import org.javamaster.idea.redis.model.DataKeys;
import org.javamaster.idea.redis.utils.RedisUtils;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author yudong
 * @date 2020/7/23
 */
public class RedisTreeView implements TreeSelectionListener, Observer {

    protected JTree jTree;

    private JPanel panelLeft;
    private JTextArea textAreaRedisValue;

    private DataKeys dataKeys;
    private DefaultMutableTreeNode root;
    private DefaultTreeModel model;

    public RedisTreeView(JPanel panelLeft, JTextArea textAreaRedisValue, JTree jTree) {
        RedisUtils.observable.addObserver(this);
        this.panelLeft = panelLeft;
        this.textAreaRedisValue = textAreaRedisValue;
        this.jTree = jTree;
        this.dataKeys = new DataKeys();
    }

    public void init(String hostsPorts) {
        jTree.removeAll();
        dataKeys.clear();
        root = new DefaultMutableTreeNode(hostsPorts);
        model = new DefaultTreeModel(root);
        createTreeData();
        jTree.setModel(model);
        jTree.addTreeSelectionListener(this);
        jTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                panelLeft.updateUI();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                panelLeft.updateUI();
            }
        });

        jTree.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu textMenu = new JPopupMenu();
                    JMenuItem menuItemRefresh = new JMenuItem("刷新");
                    JMenuItem menuItemDelete = new JMenuItem("删除");
                    JMenuItem menuItemAdd = new JMenuItem("新增");
                    JMenuItem menuItemAlter = new JMenuItem("修改");
                    menuItemDelete.addActionListener(e1 -> delAction());
                    menuItemRefresh.addActionListener(e1 -> refreshAction());
                    menuItemAdd.addActionListener(e1 -> addAction());
                    menuItemAlter.addActionListener(e12 -> alterAction());
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
                    if (node.getLevel() == 1) {
                        textMenu.add(menuItemRefresh);
                        textMenu.add(menuItemAdd);
                        textMenu.add(menuItemDelete);
                    } else if (node.getLevel() == 2) {
                        textMenu.add(menuItemAlter);
                        textMenu.add(menuItemDelete);
                    }
                    textMenu.show(e.getComponent(), e.getX(), e.getY());
                } else if (e.getClickCount() == BUTTON2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
                    if (node.getLevel() == 2) {
                        textAreaRedisValue.setText(RedisUtils.getValue(dataKeys.getDb(), dataKeys.getSingleKey()));
                    }
                }
            }
        });
        for (int i = 0; i < jTree.getRowCount(); i++) {
            jTree.expandRow(i);
        }
    }

    private void createTreeData() {
        Map<String, String> redisDbInfos = RedisUtils.getRedisDbInfos();
        for (Map.Entry<String, String> entry : redisDbInfos.entrySet()) {
            DefaultMutableTreeNode db = new DefaultMutableTreeNode(entry.getKey() + "(" + entry.getValue() + ")");
            List<String> keys = RedisUtils.getRandomKeys(Integer.parseInt(entry.getKey().replace("db", "")));
            for (String key : keys) {
                db.add(new DefaultMutableTreeNode(key));
            }
            root.add(db);
        }
    }

    public void updateTreeData(String node, List<String> redisDbInfos) {
        root.removeAllChildren();
        DefaultMutableTreeNode db;
        db = new DefaultMutableTreeNode(node);
        for (String key : redisDbInfos) {
            db.add(new DefaultMutableTreeNode(key));
        }
        root.add(db);
        ((DefaultTreeModel) jTree.getModel()).reload();
        jTree.revalidate();
        for (int i = 0; i < jTree.getRowCount(); i++) {
            jTree.expandRow(i);
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        dataKeys.clear();
        List<String> keys = new ArrayList<>();
        TreePath[] selectionPaths = jTree.getSelectionPaths();
        if (selectionPaths == null) {
            return;
        }
        for (TreePath treePath : selectionPaths) {
            DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            if (node1.getLevel() == 2) {
                keys.add(node1.getUserObject().toString());
            }
        }
        dataKeys.setKeys(keys);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
        if (node.getLevel() == 1) {
            String string = node.toString();
            string = string.substring(0, string.indexOf("("));
            dataKeys.setDb(Integer.parseInt(string.replace("db", "")));
            dataKeys.setNode(node);
        } else if (node.getLevel() == 2) {
            String parent = node.getParent().toString();
            parent = parent.substring(0, parent.indexOf("("));
            dataKeys.setDb(Integer.parseInt(parent.replace("db", "")));
            dataKeys.setSingleKey(node.getUserObject().toString());
            dataKeys.setNode(node);
        }
    }

    public void addAction() {
        JDialog dialog = new JDialog((Frame) null, "提示", true);
        dialog.setSize(350, 150);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1));
        JLabel messageLabel = new JLabel("如果第三个输入框不为空则作为hash来存储");
        panel.add(messageLabel);
        JTextField keyField = new JTextField();
        panel.add(keyField);
        JTextField valueOrHkeyField = new JTextField();
        panel.add(valueOrHkeyField);
        JTextField hvalueField = new JTextField();
        panel.add(hvalueField);
        JButton okBtn = new JButton("确定");
        panel.add(okBtn);
        okBtn.addActionListener(e2 -> {
            String key = keyField.getText();
            String valueOrHkey = valueOrHkeyField.getText();
            String hvalue = hvalueField.getText();
            RedisUtils.set(dataKeys.getDb(), key, valueOrHkey, hvalue);
            dialog.dispose();
            refreshAction();
        });
        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    public void delAction() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
        if (dataKeys.getKeys() == null) {
            return;
        }
        List<String> keys = dataKeys.getKeys();
        if (node.getLevel() == 1) {
            int res = JOptionPane.showConfirmDialog(panelLeft, "确认删除该db下的所有key?",
                    "alert", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                RedisUtils.deleteAllKeys(dataKeys.getDb());
                refreshAction();
            }
        } else if (node.getLevel() == 2) {
            RedisUtils.deleteKey(dataKeys.getDb(), keys.toArray(new String[0]));
            model.removeNodeFromParent(dataKeys.getNode());
            refreshAction();
        }
    }

    public void alterAction() {
        String s = JOptionPane.showInputDialog("请输入新的值:");
        if (s != null) {
            RedisUtils.set(dataKeys.getDb(), dataKeys.getKeys().get(0), s, null);
            refreshAction();
        }
    }

    public void refreshAction() {
        root.removeAllChildren();
        createTreeData();
        model.reload();
        jTree.revalidate();
        for (int i = 0; i < jTree.getRowCount(); i++) {
            jTree.expandRow(i);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        jTree.removeAll();
        panelLeft.updateUI();
        textAreaRedisValue.setText("");
    }
}