/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.e1c.edt.ai.AIState;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.google.inject.Inject;

/**
 *
 * @author Bogdan Sushkov
 *
 */
public class BaseStatusBarControl
    extends WorkbenchWindowControlContribution
    implements IStateListener, DisposeListener, SelectionListener
{
    @Inject
    private IStateService stateService;
    @Inject
    private IDispatcher dispatcher;
    @Inject
    private IVersionProvider versionProvider;
    @Inject
    private ISettings settings;
    @Inject
    private ISettingsSetter settingsSetter;
    @Inject
    private IReflection reflection;
    @Inject
    private IThemeManager themeManager;
    @Inject
    private IWeb web;

    private final CodeCompletionPolicy[] policies;
    private final String[] policyNames;
    private Font font;
    private Canvas statusCanvas;
    private CCombo policyCombo;
    private DefaultToolTip policyTooltip;
    private org.eclipse.swt.widgets.Menu policyMenu;

    // Status colors (soft, not too bright)
    private static final RGB COLOR_ONLINE = new RGB(120, 180, 120);
    private static final RGB COLOR_BUSY = new RGB(150, 210, 150);
    private static final RGB COLOR_OFF = new RGB(180, 120, 120); // Red for error state
    private static final RGB COLOR_DISABLED = new RGB(150, 150, 150); // Gray for disabled/off mode

    private static final int ICON_SIZE = 12;
    private static final int ICON_CORNER_RADIUS = 3;
    private static final int STATUS_TEXT_MARGIN = 5;

    private Color colorOnline;
    private Color colorBusy;
    private Color colorOff;
    private Color colorDisabled;

    private RGB currentStatusColor = COLOR_DISABLED;
    private String statusText = Messages.AIName + " "; //$NON-NLS-1$

    // Bounds for policy text click detection
    private int policyTextX = 0;
    private int policyTextWidth = 0;

    // Track if we're in MISSING_TOKEN state
    private boolean isMissingTokenState = false;

    public BaseStatusBarControl()
    {
        BaseActivator.injectMembers(this);
        policies = new CodeCompletionPolicy[CodeCompletionPolicy.values().length];
        policyNames = new String[CodeCompletionPolicy.values().length];
        for (var codeCompletionPolicy : CodeCompletionPolicy.values())
        {
            policies[codeCompletionPolicy.getIndex()] = codeCompletionPolicy;
            policyNames[codeCompletionPolicy.getIndex()] = codeCompletionPolicy.getName().toLowerCase();
        }
    }

    @Override
    protected Control createControl(Composite parent)
    {
        var composite = new Composite(parent, SWT.NONE);
        var gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 2;
        gridLayout.marginHeight = 0;
        gridLayout.marginTop = 0;
        gridLayout.marginBottom = 0;
        gridLayout.verticalSpacing = 0;
        composite.setLayout(gridLayout);

        // Status Canvas (icon + text)
        statusCanvas = new Canvas(composite, SWT.NONE);
        statusCanvas.addPaintListener(new PaintListener()
        {
            @Override
            public void paintControl(PaintEvent e)
            {
                paintStatus(e.gc);
            }
        });
        statusCanvas.addListener(SWT.MouseUp, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                onStatusCanvasClick(event);
            }
        });

        // Add resize listener to ensure proper redraw
        statusCanvas.addListener(SWT.Resize, new Listener()
        {
            @Override
            public void handleEvent(Event event)
            {
                statusCanvas.redraw();
            }
        });

        var canvasGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        canvasGridData.widthHint = 120;
        statusCanvas.setLayoutData(canvasGridData);

        // Create font for status text
        var defaultFont = statusCanvas.getFont();
        var fontData = defaultFont.getFontData()[0];
        fontData.setHeight((int)(fontData.getHeight() * 0.9));
        font = new Font(defaultFont.getDevice(), fontData);
        statusCanvas.setFont(font);

        // Initialize colors
        colorOnline = new Color(parent.getDisplay(), COLOR_ONLINE);
        colorBusy = new Color(parent.getDisplay(), COLOR_BUSY);
        colorOff = new Color(parent.getDisplay(), COLOR_OFF);
        colorDisabled = new Color(parent.getDisplay(), COLOR_DISABLED);

        // Policy Combo (create with parent to avoid taking space in layout)
        policyCombo = new CCombo(parent, SWT.READ_ONLY);
        policyCombo.setFont(font);
        policyCombo.setItems(policyNames);
        var policy = settings.getCodeCompletionPolicy();
        policyCombo.select(policy.getIndex());
        policyCombo.addSelectionListener(this);
        policyTooltip = new DefaultToolTip(policyCombo);
        policyTooltip.setText(policy.getDescription());
        policyTooltip.setHideOnMouseDown(true);
        policyTooltip.setPopupDelay(500);
        policyTooltip.setHideDelay(5000);
        policyTooltip.activate();
        try
        {
            reflection.getField(CCombo.class, policyCombo, "list", List.class).ifPresent(list -> { //$NON-NLS-1$
                list.addMouseMoveListener(new MouseMoveListener()
                {
                    @SuppressWarnings("nls")
                    @Override
                    public void mouseMove(MouseEvent e)
                    {
                        int itemHeight = policyCombo.getItemHeight();
                        if (itemHeight == 0)
                        {
                            return;
                        }

                        Integer index = (e.y - policyCombo.getBounds().y) / itemHeight;
                        if (index < 0 || index >= policies.length)
                        {
                            return;
                        }

                        policyCombo.select(index);
                        list.redraw();
                        if (index.equals(policyTooltip.getData("index")))
                        {
                            return;
                        }

                        var codeCompletionPolicy = policies[index];
                        policyTooltip.setText(codeCompletionPolicy.getDescription());
                        policyTooltip.setData("index", index);
                        var comboBounds = policyCombo.getBounds();
                        var listBounds = list.getBounds();
                        policyTooltip.show(new Point(0, -(comboBounds.height + listBounds.height + 90)));
                    }
                });

                list.getParent().addListener(SWT.Hide, new Listener()
                {
                    @Override
                    public void handleEvent(Event event)
                    {
                        policyTooltip.hide();
                    }
                });
            });
        }
        catch (Exception e)
        {
            //
        }

        // Create popup menu for policy selection
        policyMenu = new org.eclipse.swt.widgets.Menu(statusCanvas);
        for (var codeCompletionPolicy : policies)
        {
            var menuItem = new org.eclipse.swt.widgets.MenuItem(policyMenu, SWT.NONE);
            menuItem.setText(codeCompletionPolicy.getName().toLowerCase());
            menuItem.addListener(SWT.Selection, new Listener()
            {
                @Override
                public void handleEvent(Event event)
                {
                    settingsSetter.setCodeCompletionPolicy(codeCompletionPolicy);
                    policyTooltip.setText(codeCompletionPolicy.getDescription());

                    // Update color if OFF is selected
                    if (codeCompletionPolicy == CodeCompletionPolicy.OFF)
                    {
                        currentStatusColor = COLOR_DISABLED;
                    }
                    else if (settings.isEnabled())
                    {
                        currentStatusColor = COLOR_ONLINE;
                    }

                    statusCanvas.redraw();
                }
            });
        }

        policyCombo.setVisible(false); // Hide combo, will be shown programmatically

        parent.getParent().setRedraw(true);
        composite.addDisposeListener(this);

        // Force redraw with delay to ensure layout is complete
        dispatcher.dispatch(() -> {
            statusCanvas.redraw();
        });

        stateService.addListener(this);
        onStateChange(stateService.getState());
        return composite;
    }

    @SuppressWarnings("nls")
    private void paintStatus(GC gc)
    {
        var bounds = statusCanvas.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0)
        {
            return;
        }

        var display = statusCanvas.getDisplay();

        // Set antialias for smoother drawing
        gc.setAntialias(SWT.ON);
        gc.setTextAntialias(SWT.ON);

        // Get current color
        Color statusColor = getCurrentStatusColor();

        // Calculate vertical center
        int centerY = bounds.height / 2;
        int iconY = centerY - ICON_SIZE / 2;

        // Draw rounded square icon
        gc.setBackground(statusColor);
        gc.fillRoundRectangle(0, iconY, ICON_SIZE, ICON_SIZE, ICON_CORNER_RADIUS, ICON_CORNER_RADIUS);

        // Draw status text (color adapts to theme)
        Color brightForeground;
        if (themeManager.isDarkTheme())
        {
            brightForeground = new Color(display, 220, 220, 220); // Light gray for dark theme
        }
        else
        {
            brightForeground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND); // System color for light theme
        }
        gc.setForeground(brightForeground);
        gc.setFont(font);

        // Ensure font is set
        if (font != null && !font.isDisposed())
        {
            gc.setFont(font);
        }

        var textExtent = gc.textExtent(statusText);
        int textX = ICON_SIZE + STATUS_TEXT_MARGIN;
        int textY = centerY - textExtent.y / 2;

        gc.drawText(statusText, textX, textY, SWT.DRAW_TRANSPARENT);

        if (themeManager.isDarkTheme())
        {
            brightForeground.dispose(); // Dispose temporary color only if we created it
        }

        // Draw policy text and dropdown indicator (or activation link when MISSING_TOKEN)
        String policyText = ""; //$NON-NLS-1$
        int policyTextX = textX + textExtent.x;
        var policyTextExtent = gc.textExtent(policyText);

        if (isMissingTokenState)
        {
            // Show activation link instead of policy
            policyText = Messages.Activate;
            policyTextExtent = gc.textExtent(policyText);
            int policyTextY = centerY - policyTextExtent.y / 2;

            // Store bounds for click detection
            this.policyTextX = policyTextX;
            this.policyTextWidth = policyTextExtent.x;

            // Draw activation text as link (bright blue visible in dark theme)
            Color brightLink = new Color(display, 100, 200, 255); // Bright cyan/blue
            gc.setForeground(brightLink);
            gc.drawText(policyText, policyTextX, policyTextY, SWT.DRAW_TRANSPARENT);

            // Draw underline for link effect
            int underlineY = policyTextY + policyTextExtent.y - 2;
            gc.drawLine(policyTextX, underlineY, policyTextX + policyTextExtent.x, underlineY);

            brightLink.dispose(); // Dispose temporary color
        }
        else
        {
            // Show policy text as usual
            var policy = settings.getCodeCompletionPolicy();
            String policyName = policy.getName().toLowerCase();
            if (policyName.contains(":"))
            {
                // Get text after last colon
                policyText = policyName.substring(policyName.lastIndexOf(":") + 1).trim();
            }
            else
            {
                policyText = policyName;
            }

            policyTextExtent = gc.textExtent(policyText);
            int policyTextY = centerY - policyTextExtent.y / 2;

            // Store bounds for click detection
            this.policyTextX = policyTextX;
            this.policyTextWidth = policyTextExtent.x; // Text width only (no triangle)

            // Draw policy text as link (bright blue visible in dark theme)
            Color brightLink = new Color(display, 100, 200, 255); // Bright cyan/blue
            gc.setForeground(brightLink);
            gc.drawText(policyText, policyTextX, policyTextY, SWT.DRAW_TRANSPARENT);

            // Draw underline for link effect
            int underlineY = policyTextY + policyTextExtent.y - 2;
            gc.drawLine(policyTextX, underlineY, policyTextX + policyTextExtent.x, underlineY);

            brightLink.dispose(); // Dispose temporary color
        }

        // Reset foreground color
        gc.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
    }

    private void onStatusCanvasClick(Event event)
    {
        var bounds = statusCanvas.getBounds();
        if (event.x >= policyTextX && event.x <= policyTextX + policyTextWidth && event.y >= 0
            && event.y <= bounds.height)
        {
            if (isMissingTokenState)
            {
                // Open browser to activation page
                web.browse(settings.getHomePage());
            }
            else
            {
                // Show policy menu at cursor position
                var location = statusCanvas.toDisplay(event.x, event.y);
                policyMenu.setLocation(location.x, location.y);
                policyMenu.setVisible(true);
            }
        }
    }

    private Color getCurrentStatusColor()
    {
        if (colorOnline == null || colorBusy == null || colorOff == null || colorDisabled == null)
        {
            return statusCanvas.getDisplay().getSystemColor(SWT.COLOR_GRAY);
        }

        if (currentStatusColor == COLOR_ONLINE)
        {
            return colorOnline;
        }
        else if (currentStatusColor == COLOR_BUSY)
        {
            return colorBusy;
        }
        else if (currentStatusColor == COLOR_DISABLED)
        {
            return colorDisabled;
        }
        else
        {
            return colorOff;
        }
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    @Override
    public void widgetDisposed(DisposeEvent e)
    {
        stateService.removeListener(this);

        if (font != null && !font.isDisposed())
        {
            font.dispose();
        }

        if (colorOnline != null && !colorOnline.isDisposed())
        {
            colorOnline.dispose();
        }

        if (colorBusy != null && !colorBusy.isDisposed())
        {
            colorBusy.dispose();
        }

        if (colorOff != null && !colorOff.isDisposed())
        {
            colorOff.dispose();
        }

        if (colorDisabled != null && !colorDisabled.isDisposed())
        {
            colorDisabled.dispose();
        }
    }

    @Override
    public void onStateChange(AIState state)
    {
        dispatcher.dispatch(() -> changeState(state));
    }

    private void changeState(AIState state)
    {
        var info = versionProvider.getPluginVersion().toString();
        isMissingTokenState = state.getServiceState() == ServiceState.MISSING_TOKEN;

        if (settings.isEnabled())
        {
            switch (state.getServiceState())
            {
            case ONLINE:
                info = info + ' ' + Messages.StatusOnline;
                switch (state.getActionState())
                {
                case BUSY:
                    currentStatusColor = COLOR_BUSY;
                    break;

                default:
                    currentStatusColor = COLOR_ONLINE;
                    break;
                }
                break;

            case MISSING_TOKEN:
                currentStatusColor = COLOR_DISABLED;
                break;

            default:
                currentStatusColor = COLOR_OFF;
                break;
            }
        }
        else
        {
            currentStatusColor = COLOR_DISABLED;
        }

        var policy = settings.getCodeCompletionPolicy();
        statusCanvas.setToolTipText(info);
        policyCombo.select(policy.getIndex());
        policyTooltip.setText(policy.getDescription());
        statusCanvas.redraw();
    }

    @Override
    public void widgetSelected(SelectionEvent e)
    {
        policyTooltip.hide();
        var index = policyCombo.getSelectionIndex();
        if (index < 0 && index >= policies.length)
        {
            return;
        }

        var codeCompletionPolicy = policies[index];
        settingsSetter.setCodeCompletionPolicy(codeCompletionPolicy);
        policyTooltip.setText(codeCompletionPolicy.getDescription());
        if (!settings.isEnabled())
        {
            currentStatusColor = COLOR_OFF;
            statusCanvas.redraw();
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent e)
    {
        //
    }
}
