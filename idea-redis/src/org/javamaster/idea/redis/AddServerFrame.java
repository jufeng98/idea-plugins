package org.javamaster.idea.redis;

import com.intellij.openapi.ui.Messages;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import org.javamaster.idea.redis.model.RedisInfo;
import org.javamaster.idea.redis.utils.RedisUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * @author yudong
 * @date 2020/7/24
 */
public class AddServerFrame extends JFrame implements Observer {
    private JPanel panel;

    public AddServerFrame(RedisFrame redisFrame) throws HeadlessException {
        super("新增服务器");
        RedisUtils.observable.addObserver(this);
        panel = new JPanel();
        add(panel, BorderLayout.CENTER);
        initRedisList();
        JButton addRedisButton = new JButton("新增");
        add(addRedisButton, BorderLayout.NORTH);
        addRedisButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String serverInfo = JOptionPane
                        .showInputDialog("单机格式: 127.0.0.1:6379|123456 \r\n集群格式: 127.0.0.1:6379,127.0.0.1:6479|123456");
                RedisUtils.addServer(serverInfo);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() != VK_ESCAPE) {
                    return;
                }
                AddServerFrame.this.setVisible(false);
            }
        });
        setSize(new Dimension(900, 600));
        setLocationRelativeTo(redisFrame);
        setFocusable(true);
    }

    private void initRedisList() {
        List<RedisInfo> redisInfos = RedisUtils.getRedisInfos();
        panel.removeAll();
        for (int i = 0; i < redisInfos.size(); i++) {
            JPanel innerPanel = new JPanel();
            panel.add(innerPanel);
            String tmp = redisInfos.get(i).isCluster() ? "集群" : "";
            JLabel jLabel = new JLabel("服务器" + tmp + i);
            innerPanel.add(jLabel);
            JTextField textField = new JTextField(redisInfos.get(i).getHosts() + ":" + redisInfos.get(i).getPorts());
            textField.setColumns(24);
            innerPanel.add(textField);
            JPasswordField passwordField = new JPasswordField(redisInfos.get(i).getPassword());
            passwordField.setColumns(10);
            innerPanel.add(passwordField);
            JButton delButton = new JButton("删除");
            innerPanel.add(delButton);
            int finalIndex = i;
            delButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    RedisUtils.delServer(finalIndex);
                }
            });
            JButton defButton = new JButton("设为默认");
            innerPanel.add(defButton);
            defButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    RedisUtils.setDefaultServer(finalIndex);
                    Messages.showMessageDialog("设置成功", "提示", Messages.getInformationIcon());
                }
            });
        }
        panel.updateUI();
    }

    @Override
    public void update(Observable o, Object arg) {
        initRedisList();
    }
}
