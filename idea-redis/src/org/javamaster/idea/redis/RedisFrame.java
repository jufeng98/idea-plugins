package org.javamaster.idea.redis;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import org.javamaster.idea.redis.model.RedisInfo;
import org.javamaster.idea.redis.utils.RedisUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;


/**
 * @author yudong
 * @date 2020/7/23
 */
public class RedisFrame extends JFrame implements Observer {

    private static Integer width;
    private static Integer height;
    private static int currentChooseDbIndex = -1;

    static {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        GraphicsConfiguration[] gc = gs[0].getConfigurations();
        Rectangle bounds = gc[0].getBounds();
        width = new Double(bounds.getWidth()).intValue() / 2 + 600;
        height = new Double(bounds.getHeight()).intValue() / 2 + 100;
    }

    private JTextField textFieldSearch;
    private JTextArea textAreaRedisValue;
    private JTextArea textAreaHosts;
    private JPasswordField passwordFieldRedis;
    private JComboBox<String> comboBoxDbs;
    private JTextField textFieldExecute;
    private ComboBox<String> comboBoxHostsPorts;
    private RedisTreeView redisTreeView;
    private Map<String, RedisInfo> hostsPortsRedisInfoMap;

    private RedisFrame(String title) {
        super(title);
        setSize(width, height);
        RedisUtils.observable.addObserver(this);
        JPanel panelContent = new JPanel();
        setContentPane(panelContent);
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setHgap(0);
        borderLayout.setVgap(0);
        panelContent.setLayout(borderLayout);
        panelContent.setAlignmentX(20);
        panelContent.setAlignmentY(3);

        JPanel panelTop = new JPanel();
        panelContent.add(panelTop, BorderLayout.NORTH);
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        panelTop.setLayout(flowLayout);
        flowLayout.setHgap(1);
        flowLayout.setVgap(0);
        flowLayout.setAlignment(FlowLayout.LEFT);

        JLabel ipLabel = new JLabel("hosts");
        panelTop.add(ipLabel);

        hostsPortsRedisInfoMap = new HashMap<>();

        comboBoxHostsPorts = new ComboBox<>();
        comboBoxHostsPorts.setMinimumAndPreferredWidth(150);
        panelTop.add(comboBoxHostsPorts);

        textAreaHosts = new JTextArea();
        textAreaHosts.setRows(3);
        textAreaHosts.setColumns(20);
        textAreaHosts.setLineWrap(true);
        textAreaHosts.setEditable(false);
        panelTop.add(textAreaHosts);

        JLabel labelPwd = new JLabel("pwd");
        panelTop.add(labelPwd);

        passwordFieldRedis = new JPasswordField();
        passwordFieldRedis.setEditable(false);
        passwordFieldRedis.setColumns(12);
        panelTop.add(passwordFieldRedis);

        refreshComboHostsPosts();

        JButton buttonOk = new JButton("确定");
        panelTop.add(buttonOk);
        buttonOk.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String hostsPorts = textAreaHosts.getText();
                char[] password = passwordFieldRedis.getPassword();
                if (StringUtil.isEmpty(hostsPorts)) {
                    return;
                }
                RedisUtils.initServer(hostsPorts, password);
                redisTreeView.init(hostsPorts);
                Map<String, String> redisDbInfos = RedisUtils.getRedisDbInfos();
                redisDbInfos.forEach((k, v) -> comboBoxDbs.addItem(k));
                currentChooseDbIndex = 0;
            }
        });

        comboBoxDbs = new ComboBox<>();
        ((ComboBox<String>) comboBoxDbs).setMinimumAndPreferredWidth(80);
        comboBoxDbs.addActionListener(e -> {
            String selectedItem = (String) comboBoxDbs.getSelectedItem();
            if (StringUtil.isEmpty(selectedItem)) {
                return;
            }
            currentChooseDbIndex = Integer.parseInt(selectedItem.replace("db", ""));
        });
        panelTop.add(comboBoxDbs);

        textFieldSearch = new JTextField();
        textFieldSearch.setColumns(15);
        panelTop.add(textFieldSearch);

        JButton buttonSearch = new JButton("搜索");
        panelTop.add(buttonSearch);
        buttonSearch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (RedisUtils.notInitialize()) {
                    Messages.showMessageDialog("请先连接服务器", "提示", Messages.getInformationIcon());
                    return;
                }
                String key = textFieldSearch.getText();
                if (StringUtil.isNotEmpty(key)) {
                    Set<String> data = RedisUtils.getKeys(currentChooseDbIndex, key);
                    redisTreeView.updateTreeData("db" + currentChooseDbIndex + "(" + data.size() + ")", new ArrayList<>(data));
                } else {
                    redisTreeView.refreshAction();
                }
            }
        });

        JButton buttonConfigRedis = new JButton("配置服务器");
        panelTop.add(buttonConfigRedis);
        buttonConfigRedis.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AddServerFrame addServerFrame = new AddServerFrame(RedisFrame.this);
                addServerFrame.setVisible(true);
            }
        });

        textFieldExecute = new JTextField();
        textFieldExecute.setColumns(15);
        panelTop.add(textFieldExecute);
        JButton executeButton = new JButton("直接执行命令(单机)");
        panelTop.add(executeButton);
        executeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (RedisUtils.notInitialize()) {
                    Messages.showMessageDialog("请先连接服务器", "提示", Messages.getInformationIcon());
                    return;
                }
                String command = textFieldExecute.getText();
                if (StringUtil.isEmpty(command)) {
                    return;
                }
                String res = RedisUtils.exec(command, currentChooseDbIndex);
                textAreaRedisValue.setText(res);
            }
        });

        JPanel panelLeft = new JPanel();
        panelLeft.setLayout(flowLayout);

        JScrollPane redisDbScrollPanel = new JBScrollPane(panelLeft);
        panelContent.add(redisDbScrollPanel, BorderLayout.WEST);
        redisDbScrollPanel.setPreferredSize(new Dimension(getWidth() / 2, getHeight() - 55));
        redisDbScrollPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JTree dbTree = new Tree();
        panelLeft.add(dbTree);
        dbTree.setEditable(false);
        dbTree.setPreferredSize(new Dimension(getWidth() / 2, getHeight()));

        JPanel panelRedisValue = new JPanel();
        JScrollPane redisValueScrollPane = new JBScrollPane(panelRedisValue);
        redisValueScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelContent.add(redisValueScrollPane, BorderLayout.CENTER);

        textAreaRedisValue = new JTextArea();
        panelRedisValue.add(textAreaRedisValue);
        textAreaRedisValue.setLineWrap(true);
        textAreaRedisValue.setWrapStyleWord(true);
        textAreaRedisValue.setPreferredSize(new Dimension(getWidth() / 2 - 50, getHeight()));
        textAreaRedisValue.setText("暂无数据");

        redisTreeView = new RedisTreeView(panelLeft, textAreaRedisValue, dbTree);

        setLocationRelativeTo(null);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() != VK_ESCAPE) {
                    return;
                }
                RedisUtils.release();
                RedisFrame.this.setVisible(false);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                RedisUtils.release();
            }
        });
    }

    public static void main(String[] args) {
        createFrame("Redis Explorer");
    }

    static void createFrame(String text) {
        RedisFrame redisFrame = new RedisFrame(text);
        redisFrame.setVisible(true);
    }

    public void refreshComboHostsPosts() {
        List<RedisInfo> redisInfos = RedisUtils.getRedisInfos();
        RedisInfo defaultRedisInfo = redisInfos.get(0);
        textAreaHosts.setText(defaultRedisInfo.getHosts() + ":" + defaultRedisInfo.getPorts());
        passwordFieldRedis.setText(defaultRedisInfo.getPassword());
        hostsPortsRedisInfoMap.clear();
        for (RedisInfo info : redisInfos) {
            hostsPortsRedisInfoMap.put(info.getHosts() + ":" + info.getPorts(), info);
        }
        comboBoxHostsPorts.removeAllItems();
        for (RedisInfo info : redisInfos) {
            comboBoxHostsPorts.addItem(info.getHosts() + ":" + info.getPorts());
            comboBoxHostsPorts.addItemListener((ItemEvent e) -> {
                String hostsPosts = ((String) e.getItem());
                RedisInfo redisInfo = hostsPortsRedisInfoMap.get(hostsPosts);
                textAreaHosts.setText(redisInfo.getHosts() + ":" + redisInfo.getPorts());
                passwordFieldRedis.setText(redisInfo.getPassword());
            });
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        refreshComboHostsPosts();
        RedisUtils.release();
    }
}
