// Copyright (c) 2020, 2023, 2025 William Arthur Hood
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished
// to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
// HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.

package io.github.william_hood.changescan

import io.github.william_hood.boolog_kotlin.Boolog
import io.github.william_hood.toolbox_kotlin.stdout
import java.util.*


internal class FileSystemComparison(activityLog: Boolog, original: FileSystemDescription, candidate: FileSystemDescription) {
    // New To Candidate
    val newToCandidate = ArrayList<FileDescription>()

    // Removed From Original
    val removedInCandidate = ArrayList<FileDescription>()

    // Moved From Original
    val movedInCandidate = ArrayList<FileDescription>()

    // Different
    val contentDifferences = HashMap<String, FileComparison>()
    val timestampDifferences = HashMap<String, FileComparison>()

    init {
        // Given a description of an original and candidate file system, populate the categories of differences between the two.
        for (originalFileDescription in original.fileDescriptions) {
            activityLog.info("Comparing ${originalFileDescription.fullyQualifiedPath}")
            val counterpart = candidate.pop(originalFileDescription.fullyQualifiedPath)
            if (counterpart == null) {
                activityLog.info(" • Counting as Removed in Candidate: ${originalFileDescription.fullyQualifiedPath}")
                removedInCandidate.add(originalFileDescription)
            } else {
                val fileComparison = FileComparison(originalFileDescription, counterpart)
                if (fileComparison.differences.size > 0) {
                    if (fileComparison.contentWasChanged) {
                        activityLog.info(" • Counting as content change: ${originalFileDescription.fullyQualifiedPath}")
                        contentDifferences[fileComparison.fullyQualifiedPath] = fileComparison
                    } else {
                        activityLog.info(" • Counting as timestamp change: ${originalFileDescription.fullyQualifiedPath}")
                        timestampDifferences[fileComparison.fullyQualifiedPath] = fileComparison
                    }
                }
            }
        }

        // At this point, only new files should remain in the Candidate
        for (newFileDescription in candidate.fileDescriptions) {
            if (appearsMoved(newFileDescription)) {
                activityLog.info(" • Counting as Moved in Candidate: ${newFileDescription.fullyQualifiedPath}")
                movedInCandidate.add(newFileDescription)
            } else {
                activityLog.info(" • Counting as New to Candidate: ${newFileDescription.fullyQualifiedPath}")
                newToCandidate.add(newFileDescription)
            }
        }
    }

    private fun appearsMoved(newFileDescription: FileDescription): Boolean {
        removedInCandidate.forEach {
            if (it.isTheSameFile(newFileDescription)) {
                newFileDescription.formerDirectory = it.directory
                removedInCandidate.remove(it)
                return true
            }
        }

        return false
    }
}
