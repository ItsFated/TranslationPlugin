package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.*;
import cn.yiiguxing.plugin.translate.action.AutoSelectionMode;
import cn.yiiguxing.plugin.translate.compat.IdeaCompat;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.*;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;

/**
 * 设置页
 */
@SuppressWarnings("Since15")
public class SettingsPanel {

    private static final String DEFAULT_PASSWORD_TEXT = "********************************";
    private static final boolean KEE_PASS_SUPPORT = IdeaCompat.BUILD_NUMBER >= IdeaCompat.Version.IDEA2016_3;

    private static final int INDEX_INCLUSIVE = 0;
    private static final int INDEX_EXCLUSIVE = 1;

    private JPanel mWholePanel;
    private JPanel mLangSettingsPanel;
    private JPanel mSelectionSettingsPanel;
    private JPanel mApiKeySettingsPanel;

    private JComboBox<String> mSelectionMode;
    private JTextField mAppIdField;
    private JPasswordField mAppPrivateKeyField;
    private LinkLabel mGetApiKeyLink;
    private JPanel mHistoryPanel;
    private ComboBox mMaxHistoriesSize;
    private JButton mClearHistoriesButton;
    private JPanel mFontPanel;
    private JCheckBox mFontCheckBox;
    private FontComboBox mPrimaryFontComboBox;
    private FontComboBox mPhoneticFontComboBox;
    private JTextPane mFontPreview;
    private JLabel mPrimaryFontLabel;
    private JLabel mPhoneticFontLabel;
    private JComboBox<Lang> mLangFromComboBox;
    private JComboBox<Lang> mLangToComboBox;

    private Settings mSettings;
    private AppStorage mAppStorage;

    private boolean mPrivateKeyModified;

    public JComponent createPanel(@NotNull Settings settings, @NotNull AppStorage appStorage) {
        mSettings = settings;
        mAppStorage = appStorage;

        setTitles();
        setRenderer();
        setListeners();

        return mWholePanel;
    }

    private void createUIComponents() {
        mGetApiKeyLink = new ActionLink("", new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                BrowserUtil.browse(Constants.YOUDAO_AI_URL);
            }
        });
        mGetApiKeyLink.setIcon(AllIcons.Ide.Link);

        mPrimaryFontComboBox = new FontComboBox();
        if (IdeaCompat.BUILD_NUMBER >= IdeaCompat.Version.IDEA2017_1) {
            mPhoneticFontComboBox = new FontComboBox(false);
        } else {
            mPhoneticFontComboBox = new FontComboBox();
        }

        fixFontComboBoxSize(mPrimaryFontComboBox);
        fixFontComboBoxSize(mPhoneticFontComboBox);

        final List<Lang> languages = Arrays.asList(Lang.values());
        mLangFromComboBox = new ComboBox<Lang>(new CollectionComboBoxModel<Lang>(languages));
        mLangToComboBox = new ComboBox<Lang>(new CollectionComboBoxModel<Lang>(languages));
    }

    private void fixFontComboBoxSize(FontComboBox fontComboBox) {
        Dimension size = fontComboBox.getPreferredSize();
        size.width = size.height * 8;
        fontComboBox.setPreferredSize(size);
    }

    private void setTitles() {
        mLangSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("语言"));
        mSelectionSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("取词模式"));
        mFontPanel.setBorder(IdeBorderFactory.createTitledBorder("字体"));
        mHistoryPanel.setBorder(IdeBorderFactory.createTitledBorder("历史记录"));
        mApiKeySettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("有道翻译"));
    }

    private void setRenderer() {
        mSelectionMode.setRenderer(new ListCellRendererWrapper<String>() {
            @Override
            public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
                setText(value);
                if (index == INDEX_INCLUSIVE) {
                    setToolTipText("以最大范围取最近的所有词");
                } else if (index == INDEX_EXCLUSIVE) {
                    setToolTipText("取最近的单个词");
                }
            }
        });

        final ListCellRendererWrapper<Lang> langRenderer = new ListCellRendererWrapper<Lang>() {
            @Override
            public void customize(JList list, Lang value, int index, boolean selected, boolean hasFocus) {
                setText(value.langName);
            }
        };
        mLangFromComboBox.setRenderer(langRenderer);
        mLangToComboBox.setRenderer(langRenderer);
    }

    private void setListeners() {
        mFontCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final boolean selected = mFontCheckBox.isSelected();
                mPrimaryFontComboBox.setEnabled(selected);
                mPhoneticFontComboBox.setEnabled(selected);
                mFontPreview.setEnabled(selected);
                mPrimaryFontLabel.setEnabled(selected);
                mPhoneticFontLabel.setEnabled(selected);
            }
        });
        mPrimaryFontComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    previewPrimaryFont(mPrimaryFontComboBox.getFontName());
                }
            }
        });
        mPhoneticFontComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    previewPhoneticFont(mPhoneticFontComboBox.getFontName());
                }
            }
        });
        mClearHistoriesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mAppStorage.clearHistories();
            }
        });

        if (!KEE_PASS_SUPPORT) {
            mAppPrivateKeyField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent e) {
                    mPrivateKeyModified = true;
                }
            });
            mAppPrivateKeyField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (!mPrivateKeyModified && !getAppPrivateKey().isEmpty()) {
                        setAppPrivateKey("");
                        mPrivateKeyModified = true;
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                }
            });
        }
    }

    private void previewPrimaryFont(String primary) {
        if (Utils.isEmptyOrBlankString(primary)) {
            mFontPreview.setFont(JBUI.Fonts.label(14));
        } else {
            mFontPreview.setFont(JBUI.Fonts.create(primary, 14));
        }
    }

    private void previewPhoneticFont(String primary) {
        final StyledDocument document = mFontPreview.getStyledDocument();

        Font font;
        if (Utils.isEmptyOrBlankString(primary)) {
            font = JBUI.Fonts.label(14);
        } else {
            font = JBUI.Fonts.create(primary, 14);
        }

        final SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributeSet, font.getFamily());
        document.setCharacterAttributes(4, 41, attributeSet, true);
    }

    @NotNull
    private AutoSelectionMode getAutoSelectionMode() {
        if (mSelectionMode.getSelectedIndex() == INDEX_INCLUSIVE) {
            return AutoSelectionMode.INCLUSIVE;
        } else {
            return AutoSelectionMode.EXCLUSIVE;
        }
    }

    private int getMaxHistorySize() {
        final Object size = mMaxHistoriesSize.getEditor().getItem();
        if (size instanceof String) {
            try {
                return Integer.parseInt((String) size);
            } catch (NumberFormatException e) {
                /*no-op*/
            }
        }

        return -1;
    }

    @NotNull
    private String getAppPrivateKey() {
        return new String(mAppPrivateKeyField.getPassword());
    }

    private void setAppPrivateKey(@NotNull String key) {
        mAppPrivateKeyField.setText(key.isEmpty() ? null : key);
    }

    public boolean isModified() {
        return (!Utils.isEmptyOrBlankString(mAppIdField.getText())
                && !Utils.isEmptyOrBlankString(getAppPrivateKey())
                && (KEE_PASS_SUPPORT || mPrivateKeyModified))
                || mSettings.getAutoSelectionMode() != getAutoSelectionMode()
                || getMaxHistorySize() != mAppStorage.getMaxHistorySize()
                || mFontCheckBox.isSelected() != mSettings.isOverrideFont()
                || (mSettings.getPrimaryFontFamily() != null
                && mSettings.getPrimaryFontFamily().equals(mPrimaryFontComboBox.getFontName()))
                || (mSettings.getPhoneticFontFamily() != null
                && mSettings.getPhoneticFontFamily().equals(mPhoneticFontComboBox.getFontName()))
                || mSettings.getLangFrom() != mLangFromComboBox.getSelectedItem()
                || mSettings.getLangTo() != mLangToComboBox.getSelectedItem();
    }

    public void apply() {
        mSettings.setAppId(mAppIdField.getText());
        if (KEE_PASS_SUPPORT) {
            final String appPrivateKey = getAppPrivateKey();
            if (!(appPrivateKey.equals(mSettings.getAppPrivateKey()))) {
                mSettings.setAppPrivateKey(appPrivateKey);
            }
        } else if (mPrivateKeyModified) {
            mSettings.setAppPrivateKey(getAppPrivateKey());
        }

        final int maxHistorySize = getMaxHistorySize();
        if (maxHistorySize >= 0) {
            mAppStorage.setMaxHistorySize(maxHistorySize);
        }

        mSettings.setLangFrom((Lang) mLangFromComboBox.getSelectedItem());
        mSettings.setLangTo((Lang) mLangToComboBox.getSelectedItem());
        mSettings.setOverrideFont(mFontCheckBox.isSelected());
        mSettings.setPrimaryFontFamily(mPrimaryFontComboBox.getFontName());
        mSettings.setPhoneticFontFamily(mPhoneticFontComboBox.getFontName());
        mSettings.setAutoSelectionMode(getAutoSelectionMode());
    }

    public void reset() {
        mLangFromComboBox.setSelectedItem(Utils.notNull(mSettings.getLangFrom(), Lang.AUTO));
        mLangToComboBox.setSelectedItem(Utils.notNull(mSettings.getLangTo(), Lang.AUTO));
        mFontCheckBox.setSelected(mSettings.isOverrideFont());
        mPrimaryFontComboBox.setFontName(mSettings.getPrimaryFontFamily());
        mPhoneticFontComboBox.setFontName(mSettings.getPhoneticFontFamily());
        previewPrimaryFont(mSettings.getPrimaryFontFamily());
        previewPhoneticFont(mSettings.getPhoneticFontFamily());

        mAppIdField.setText(mSettings.getAppId());
        if (KEE_PASS_SUPPORT) {
            setAppPrivateKey(mSettings.getAppPrivateKey());
        } else {
            setAppPrivateKey(mSettings.isPrivateKeyConfigured() ? DEFAULT_PASSWORD_TEXT : "");
            mPrivateKeyModified = false;
        }

        mMaxHistoriesSize.getEditor().setItem(Integer.toString(mAppStorage.getMaxHistorySize()));
        mSelectionMode.setSelectedIndex(mSettings.getAutoSelectionMode() == AutoSelectionMode.INCLUSIVE
                ? INDEX_INCLUSIVE : INDEX_EXCLUSIVE);
    }
}
