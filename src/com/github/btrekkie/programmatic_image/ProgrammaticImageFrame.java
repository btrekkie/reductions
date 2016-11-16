package com.github.btrekkie.programmatic_image;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A JFrame that renders an IProgrammaticImageRenderer image, along with zoom buttons and scroll bars.
 * ProgrammaticImageFrame performs best if IProgrammaticImageRenderer.render terminates in a timely fashion when its
 * thread is interrupted, as in Thread.interrupted().
 */
public class ProgrammaticImageFrame extends JFrame {
    /** The factor by which to change the scaling factor when the user clicks a zoom button. */
    /* We use Math.pow(2, 1.0 / x) for an integer x in so that clicking the zoom in button x times perfectly doubles the
     * scaling factor.
     */
    private static final double SCALE_MULT = Math.pow(2, 1.0 / 3);

    /**
     * The minimum value to use for the visible width and height when determining a value of baseScale that will fit the
     * entire image in the visible area.
     */
    private static final int MIN_SIZE_FOR_BASE_SCALE = 100;

    private static final long serialVersionUID = -3629792110769252855L;

    /** The IProgrammaticImageRenderer whose image we are rendering. */
    private final IProgrammaticImageRenderer renderer;

    /** The ProgrammaticImageComponent that is rendering the image. */
    private final ProgrammaticImageComponent component;

    /** The scroll pane for scrolling "component". */
    private JScrollPane scrollPane;

    /** The "base" scaling factor, when scaleLevel is 0.  This is unspecified prior to computing it. */
    private double baseScale;

    /** Whether we have set a baseScale value. */
    private boolean haveInitializedBaseScale;

    /**
     * The number of zoom increments we have zoomed in relative to baseScale.  The scaling factor is
     * baseScale * Math.pow(SCALE_MULT, scaleLevel).
     */
    private int scaleLevel;

    /**
     * The current horizontal scroll position, if we are in the process of zooming in or out.  This is a negative number
     * if we are not currently zooming in or out.  This is expressed as the x coordinate of the center of the visible
     * portion of the scroll content divided by the width of the content.
     */
    private double zoomScrollX = -1;

    /**
     * The current vertical scroll position, if we are in the process of zooming in or out.  This is a negative number
     * if we are not currently zooming in or out.  This is expressed as the y coordinate of the center of the visible
     * portion of the scroll content divided by the height of the content.
     */
    private double zoomScrollY = -1;

    public ProgrammaticImageFrame(IProgrammaticImageRenderer renderer) {
        this.renderer = renderer;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // Zoom buttons
        JPanel panel = new JPanel();
        panel.setSize(200, 40);
        panel.setMaximumSize(new Dimension(200, 40));
        JButton zoomOutButton = new JButton("-");
        JButton zoomInButton = new JButton("+");
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomOut();
            }
        });
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoomIn();
            }
        });
        panel.add(zoomOutButton);
        panel.add(zoomInButton);
        add(panel);

        component = new ProgrammaticImageComponent(renderer);
        scrollPane = new JScrollPane(component);
        add(scrollPane);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                ProgrammaticImageFrame.this.componentResized();
            }
        });
        component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                imageComponentResized();
            }
        });
    }

    /** Responds to a change in scaleLevel. */
    private void updateScale() {
        Point position = scrollPane.getViewport().getViewPosition();
        Dimension size = scrollPane.getViewport().getExtentSize();
        zoomScrollX = (position.x + size.getWidth() / 2) / component.getWidth();
        zoomScrollY = (position.y + size.getHeight() / 2) / component.getHeight();
        component.setScale(baseScale * Math.pow(SCALE_MULT, scaleLevel));
    }

    /** Decreases the level of zoom by one increment. */
    private void zoomOut() {
        scaleLevel--;
        updateScale();
    }

    /** Increases the level of zoom by one increment. */
    private void zoomIn() {
        scaleLevel++;
        updateScale();
    }

    /** Responds to a change in the size of the frame. */
    private void componentResized() {
        if (!haveInitializedBaseScale) {
            // Set baseScale to be the largest power of SCALE_MULT that fits the entire image.  We make it a power of
            // SCALE_MULT so that it is possible to display the image at one pixel per unit.  Depending on the
            // IProgrammaticImageRenderer, this might be a meaningful level of zoom.
            int width = Math.max(MIN_SIZE_FOR_BASE_SCALE, component.getWidth());
            int height = Math.max(MIN_SIZE_FOR_BASE_SCALE, component.getHeight());
            int level = 0;
            while (Math.pow(SCALE_MULT, level + 1) * renderer.width() <= width &&
                    Math.pow(SCALE_MULT, level + 1) * renderer.height() <= height) {
                level++;
            }
            while (Math.pow(SCALE_MULT, level) * renderer.width() > width ||
                    Math.pow(SCALE_MULT, level) * renderer.height() > height) {
                level--;
            }
            baseScale = Math.pow(SCALE_MULT, level);

            component.setScale(baseScale);
            scaleLevel = 0;
            haveInitializedBaseScale = true;
        }
    }

    /** Responds to a change in the size of "component". */
    private void imageComponentResized() {
        if (zoomScrollX >= 0) {
            // The user just clicked a zoom button.  Scroll so that the center of the visible portion of the image
            // remains the same.
            Dimension size = scrollPane.getViewport().getExtentSize();
            double x = zoomScrollX * component.getWidth() - size.getWidth() / 2;
            double y = zoomScrollY * component.getHeight() - size.getHeight() / 2;
            int clampedX = Math.max(0, (int)(Math.min(component.getWidth() - size.getWidth(), x) + 0.5));
            int clampedY = Math.max(0, (int)(Math.min(component.getHeight() - size.getHeight(), y) + 0.5));
            scrollPane.getViewport().setViewPosition(new Point(clampedX, clampedY));
            zoomScrollX = -1;
            zoomScrollY = -1;
        }
    }
}
