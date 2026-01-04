/**
 * Copyright 2024 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opengl.test.junit.jogl.awt;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug1478Deadlock01AWT extends UITestCase {
    @Test(timeout = 10000)
    public void test01SetVisiblePlainThenGLCanvas() throws InterruptedException, InvocationTargetException {
        Assume.assumeTrue(Platform.getOSType() == Platform.OSType.MACOS);
        Assume.assumeFalse("Headless environment", GraphicsEnvironment.isHeadless());
        Assume.assumeTrue("GL2 not available", GLProfile.isAvailable(GLProfile.GL2));

        final JFrame frame = new JFrame();
        final PhotoFrame photoFrame = new PhotoFrame();
        boolean shown = false;

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame.setSize(200, 150);
                    frame.validate();
                    frame.setVisible(true);

                    photoFrame.setSize(320, 240);
                    photoFrame.validate();
                    photoFrame.setVisible(true);
                }
            });
            shown = true;

            final GLCanvas canvas = photoFrame.getCanvas();
            Assume.assumeNotNull(canvas);

            Assume.assumeTrue("GLCanvas didn't become visible", AWTRobotUtil.waitForVisible(canvas, true, null));
            Assume.assumeTrue("GLCanvas didn't become realized", AWTRobotUtil.waitForRealized(canvas, true, null));
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        } finally {
            if( shown && !Thread.currentThread().isInterrupted() ) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            photoFrame.setVisible(false);
                            photoFrame.dispose();
                            frame.setVisible(false);
                            frame.dispose();
                        }
                    });
                } catch (final Throwable t) {
                    t.printStackTrace();
                    Assume.assumeNoException(t);
                }
            }
        }
    }

    private static class PhotoFrame extends JFrame {
        private final PhotoPanel photoPanel;

        PhotoFrame() {
            photoPanel = new PhotoPanel();
            setContentPane(photoPanel);
        }

        GLCanvas getCanvas() { return photoPanel.getCanvas(); }
    }

    private static class PhotoPanel extends JPanel implements GLEventListener {
        private GLCanvas canvas;

        PhotoPanel() {
            setLayout(new BorderLayout());
            initGLCanvas();
        }

        private void initGLCanvas() {
            try {
                final GLProfile glp = GLProfile.get(GLProfile.GL2);
                final GLCapabilities caps = new GLCapabilities(glp);
                canvas = new GLCanvas(caps);
                canvas.addGLEventListener(this);
                add(canvas, BorderLayout.CENTER);
            } catch (final Throwable t) {
                add(new JLabel("Unable to load 3d Libraries: " + t.getMessage()));
            }
        }

        GLCanvas getCanvas() { return canvas; }

        @Override
        public void paintImmediately(final Rectangle r) { }

        @Override
        public void paintImmediately(final int x, final int y, final int w, final int h) { }

        @Override
        public void display(final GLAutoDrawable drawable) { }

        @Override
        public void init(final GLAutoDrawable drawable) { }

        @Override
        public void dispose(final GLAutoDrawable drawable) { }

        @Override
        public void reshape(final GLAutoDrawable drawable, final int i, final int i1, final int i2, final int i3) { }
    }

    public static void main(final String[] args) {
        org.junit.runner.JUnitCore.main(TestBug1478Deadlock01AWT.class.getName());
    }
}

