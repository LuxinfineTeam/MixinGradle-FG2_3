/*
 * This file is part of MixinGradle, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.gradle.plugins.meta

import static org.objectweb.asm.Opcodes.*

import groovy.transform.PackageScope
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Type

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Represents an imported library for the annotation processor. Currently only
 * supports scanning for mixins and logging their targets
 */
class Import {
    
    /**
     * Imported library
     */
    File file
    
    /**
     * Discovered mixin target lines (preformatted
     */
    List<String> targets = []
    
    /**
     * True if the import was already scanned
     */
    private boolean generated = false

    Import(File file) {
        this.file = file
    }
    
    /**
     * Scan the import
     * 
     * @return fluent interface
     */
    Import read() {
        if (this.generated) {
            return this
        }
        
        if (file.file) {
            this.readFile()
        }
        
        this.generated = true
        return this
    }
    
    /**
     * Scan a file import
     */
    @PackageScope void readFile() {
        this.targets.clear()
        
        new ZipInputStream(this.file.newInputStream()).withStream { zin ->
            for (ZipEntry entry = null; (entry = zin.nextEntry) != null;) {
                if (entry.directory || !entry.name.endsWith('.class')) {
                    continue
                }
                
                // Read the inner classes from the class file
                MixinScannerVisitor mixin = new MixinScannerVisitor()
                new ClassReader(zin).accept(mixin, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)

                for (String target : mixin.targets) {
                    this.targets.add(sprintf("%s\t%s", mixin.name, target))
                }                
            }
        }
    }
    
    /**
     * Append the contents of this file to the specified writer
     * 
     * @param writer Writer to append to
     * @return fluent interface
     */
    Import appendTo(PrintWriter writer) {
        this.read()
        for (String target : this.targets) {
            writer.println(target)
        }
        return this
    }

    /**
     * ASM class visitor for scanning the classes for Mixin annotations
     */
    private static class MixinScannerVisitor extends ClassVisitor {

        /**
         * Discovered class name
         */
        String name

        List<String> targets = []

        MixinScannerVisitor() {
            super(ASM9)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.name = name
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(desc)) {
                return new MixinAnnotationVisitor(this.targets)
            }
            return super.visitAnnotation(desc, visible)
        }

        List<String> getTargets() {
            return this.targets
        }
    }

    /**
     * ASM annotation visitor for parsing Mixin annotation values
     */
    private static class MixinAnnotationVisitor extends AnnotationVisitor {

        private final List<String> targets

        MixinAnnotationVisitor(List<String> targets) {
            super(ASM5)
            this.targets = targets
        }

        @Override
        AnnotationVisitor visitArray(String name) {
            if ("value".equals(name)) {
                return new MixinTargetArrayVisitor(this.targets, true)
            }
            if ("targets".equals(name)) {
                return new MixinTargetArrayVisitor(this.targets, false)
            }
            return super.visitArray(name)
        }
    }

    /**
     * ASM annotation visitor for parsing array values (value and targets)
     */
    private static class MixinTargetArrayVisitor extends AnnotationVisitor {

        private final List<String> targets
        private final boolean isTypeArray

        MixinTargetArrayVisitor(List<String> targets, boolean isTypeArray) {
            super(ASM5)
            this.targets = targets
            this.isTypeArray = isTypeArray
        }

        @Override
        void visit(String name, Object value) {
            if (this.isTypeArray && value instanceof Type) {
                this.targets.add(((Type) value).getClassName().replace(".", "/"))
            } else if (!this.isTypeArray && value instanceof String) {
                this.targets.add(((String) value).replace(".", "/"))
            }
        }
    }
        
}
