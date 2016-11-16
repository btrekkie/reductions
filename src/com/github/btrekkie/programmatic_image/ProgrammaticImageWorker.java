package com.github.btrekkie.programmatic_image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

/** A SwingWorker for rendering a region of an IProgrammaticImageRenderer image. */
public class ProgrammaticImageWorker extends SwingWorker<BufferedImage, Object> {
    /** The IProgrammaticImageRenderer whose image we are rendering. */
    private final IProgrammaticImageRenderer renderer;

    /** The region we are rendering. */
    private final ProgrammaticImageRect rect;

    /** The component to which to report the results of the rendering. */
    private final ProgrammaticImageComponent component;

    public ProgrammaticImageWorker(
            IProgrammaticImageRenderer renderer, ProgrammaticImageRect rect, ProgrammaticImageComponent component) {
        this.renderer = renderer;
        this.rect = rect;
        this.component = component;
    }

    @Override
    protected BufferedImage doInBackground() throws IOException {
        try {
            return renderer.render(rect.scale, rect.x, rect.y, rect.width, rect.height);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw exception;
        }
    }

    @Override
    protected void done() {
        try {
            component.handleResult(get(), rect);
        } catch (InterruptedException exception) {

        } catch (ExecutionException exception) {

        } catch (CancellationException exception) {

        }
    }
}
