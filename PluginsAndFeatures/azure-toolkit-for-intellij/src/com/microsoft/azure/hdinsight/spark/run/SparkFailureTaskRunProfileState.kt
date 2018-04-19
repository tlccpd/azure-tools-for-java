/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.util.JavaParametersUtil
import com.microsoft.azure.hdinsight.spark.common.SparkFailureTaskDebugConfigurableModel
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView
import java.nio.file.Paths

open class SparkFailureTaskRunProfileState(val name: String,
                                           private val settingsConfigModel: SparkFailureTaskDebugConfigurableModel)
        : RunProfileState {
    val project = settingsConfigModel.project

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        // Leverage Spark Local Run/Debug console view
        val consoleView = SparkJobLogConsoleView(project)
        val processHandler = KillableColoredProcessHandler(createCommandLine())

        consoleView.attachToProcess(processHandler)

        return DefaultExecutionResult(consoleView, processHandler)
    }

    val failureContextPath get() = settingsConfigModel.settings.failureContextPath

    protected open val additionalVmParameters: Array<String>
        get() {
            // Failure Task Context file
            return arrayOf("-Dspark.failure.task.context=$failureContextPath")
        }

    @Throws(ExecutionException::class)
    private fun createCommandLine() : GeneralCommandLine {
        val params = JavaParameters()

        JavaParametersUtil.configureConfiguration(params, settingsConfigModel)

        // Change the working directory to the one of Spark Failure Task Context
        params.workingDirectory = Paths.get(failureContextPath).parent.toString()

        // The dependent spark-tools.jar is already in the Maven project lib/ directory
        JavaParametersUtil.configureProject(project, params, JavaParameters.JDK_AND_CLASSES_AND_TESTS, null)

        // Additional VM parameters
        additionalVmParameters.forEach { params.vmParametersList.add(it) }

        // Helper Main class
        params.mainClass = settingsConfigModel.runClass

        return params.toCommandLine()
    }
}