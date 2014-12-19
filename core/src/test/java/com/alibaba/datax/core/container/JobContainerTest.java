package com.alibaba.datax.core.container;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.job.JobContainer;
import com.alibaba.datax.core.scaffold.base.CaseInitializer;
import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.CommunicationManager;
import com.alibaba.datax.core.util.ConfigParser;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.alibaba.datax.core.util.container.LoadUtil;
import com.alibaba.datax.dataxservice.face.domain.ExecuteMode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jingxing on 14-9-2.
 */
public class JobContainerTest extends CaseInitializer {
    private Configuration configuration;

    @Before
    public void setUp() {
        String path = JobContainerTest.class.getClassLoader()
                .getResource(".").getFile();

        this.configuration = ConfigParser.parse(path + File.separator
                + "all.json");
        LoadUtil.bind(this.configuration);
    }

    /**
     * standalone模式下点对点跑完全部流程
     */
    @Test
    public void testStart() {
        JobContainer jobContainer = new JobContainer(
                this.configuration);
        jobContainer.start();
    }

    @Test(expected = Exception.class)
    public void testStartException() {
        this.configuration.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, -2);
        this.configuration.set("runMode", ExecuteMode.DISTRIBUTE.getValue());
        JobContainer jobContainer = new JobContainer(
                this.configuration);
        jobContainer.start();
    }

    @Test
    public void testInitNormal() throws Exception {
        this.configuration.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, -2);
        this.configuration.set("runMode", ExecuteMode.STANDALONE.getValue());
        JobContainer jobContainer = new JobContainer(
                this.configuration);

        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, new Object[] {});
        Assert.assertEquals("default job id = 0", 0l, this.configuration
                .getLong(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID)
                .longValue());
    }

    @Test(expected = Exception.class)
    public void testInitException() throws Exception {
        this.configuration.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, -2);
        this.configuration.set("runMode", ExecuteMode.DISTRIBUTE.getValue());
        JobContainer jobContainer = new JobContainer(
                this.configuration);
        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, new Object[] {});
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMergeReaderAndWriterSlicesConfigs() throws Exception {
        JobContainer jobContainer = new JobContainer(
                this.configuration);
        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, new Object[] {});
        initMethod.setAccessible(false);

        int splitNumber = 100;
        List<Configuration> readerSplitConfigurations = new ArrayList<Configuration>();
        List<Configuration> writerSplitConfigurations = new ArrayList<Configuration>();
        for (int i = 0; i < splitNumber; i++) {
            Configuration readerOneConfig = Configuration.newDefault();
            List<String> jdbcUrlArray = new ArrayList<String>();
            jdbcUrlArray.add(String.format(
                    "jdbc:mysql://localhost:3305/db%04d", i));
            readerOneConfig.set("jdbcUrl", jdbcUrlArray);

            List<String> tableArray = new ArrayList<String>();
            tableArray.add(String.format("jingxing_%04d", i));
            readerOneConfig.set("table", tableArray);

            readerSplitConfigurations.add(readerOneConfig);

            Configuration writerOneConfig = Configuration.newDefault();
            List<String> odpsUrlArray = new ArrayList<String>();
            odpsUrlArray.add(String.format("odps://localhost:3305/db%04d", i));
            writerOneConfig.set("jdbcUrl", odpsUrlArray);

            List<String> odpsTableArray = new ArrayList<String>();
            odpsTableArray.add(String.format("jingxing_%04d", i));
            writerOneConfig.set("table", odpsTableArray);

            writerSplitConfigurations.add(writerOneConfig);
        }

        initMethod = jobContainer.getClass().getDeclaredMethod(
                "mergeReaderAndWriterTaskConfigs", List.class, List.class);
        initMethod.setAccessible(true);

        List<Configuration> mergedConfigs = (List<Configuration>) initMethod
                .invoke(jobContainer, readerSplitConfigurations, writerSplitConfigurations);

        Assert.assertEquals("merge number equals to split number", splitNumber,
                mergedConfigs.size());
        for (Configuration sliceConfig : mergedConfigs) {
            Assert.assertNotNull("reader name not null",
                    sliceConfig.getString(CoreConstant.JOB_READER_NAME));
            Assert.assertNotNull("reader name not null",
                    sliceConfig.getString(CoreConstant.JOB_READER_PARAMETER));
            Assert.assertNotNull("reader name not null",
                    sliceConfig.getString(CoreConstant.JOB_WRITER_NAME));
            Assert.assertNotNull("reader name not null",
                    sliceConfig.getString(CoreConstant.JOB_WRITER_PARAMETER));
            Assert.assertTrue("has slice id",
                    sliceConfig.getInt(CoreConstant.JOB_TASK_ID) >= 0);
        }
    }

    @Test(expected = Exception.class)
    public void testMergeReaderAndWriterSlicesConfigsException()
            throws Exception {
        JobContainer jobContainer = new JobContainer(
                this.configuration);
        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, new Object[] {});
        initMethod.setAccessible(false);

        int readerSplitNumber = 100;
        int writerSplitNumber = readerSplitNumber + 1;
        List<Configuration> readerSplitConfigurations = new ArrayList<Configuration>();
        List<Configuration> writerSplitConfigurations = new ArrayList<Configuration>();
        for (int i = 0; i < readerSplitNumber; i++) {
            Configuration readerOneConfig = Configuration.newDefault();
            readerSplitConfigurations.add(readerOneConfig);
        }
        for (int i = 0; i < writerSplitNumber; i++) {
            Configuration readerOneConfig = Configuration.newDefault();
            writerSplitConfigurations.add(readerOneConfig);
        }

        initMethod = jobContainer.getClass().getDeclaredMethod(
                "mergeReaderAndWriterSlicesConfigs", List.class, List.class);
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, readerSplitConfigurations, writerSplitConfigurations);
    }

    @Test
    public void testDistributeTasksToTaskGroupContainer() throws Exception {
        distributeTasksToTaskGroupContainerTest(333, 7);

        distributeTasksToTaskGroupContainerTest(6, 7);
        distributeTasksToTaskGroupContainerTest(7, 7);
        distributeTasksToTaskGroupContainerTest(8, 7);

        distributeTasksToTaskGroupContainerTest(1, 1);
        distributeTasksToTaskGroupContainerTest(2, 1);
        distributeTasksToTaskGroupContainerTest(1, 2);

        distributeTasksToTaskGroupContainerTest(1, 1025);
        distributeTasksToTaskGroupContainerTest(1024, 1025);
    }

    /**
     * 分发测试函数，可根据不同的通道数、每个taskGroup平均包括的channel数得到最优的分发结果
     * 注意：默认的tasks是采用faker里切分出的1024个tasks
     *
     * @param channelNumber
     * @param channelsPerTaskGroupContainer
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private void distributeTasksToTaskGroupContainerTest(int channelNumber,
                                                         int channelsPerTaskGroupContainer) throws Exception {
        JobContainer jobContainer = new JobContainer(
                this.configuration);
        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, new Object[] {});
        initMethod.setAccessible(false);

        initMethod = jobContainer.getClass().getDeclaredMethod("split");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, new Object[] {});
        initMethod.setAccessible(false);

        int tasksNumber = this.configuration.getListConfiguration(
                CoreConstant.DATAX_JOB_CONTENT).size();
        int averSlicesPerChannel = tasksNumber / channelNumber;

        initMethod = jobContainer.getClass().getDeclaredMethod(
                "distributeTasksToTaskGroup", int.class, int.class,
                int.class);
        initMethod.setAccessible(true);
        List<Configuration> taskGroupConfigs = (List<Configuration>) initMethod
                .invoke(jobContainer, averSlicesPerChannel,
                        channelNumber, channelsPerTaskGroupContainer);
        initMethod.setAccessible(false);

        Assert.assertEquals("task size check", channelNumber
                        / channelsPerTaskGroupContainer
                        + (channelNumber % channelsPerTaskGroupContainer > 0 ? 1 : 0),
                taskGroupConfigs.size());
        int sumSlices = 0;
        for (Configuration taskGroupConfig : taskGroupConfigs) {
            Assert.assertNotNull("have set taskGroupId", taskGroupConfig
                    .getInt(CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_ID));
            int channelNo = taskGroupConfig
                    .getInt(CoreConstant.DATAX_CORE_CONTAINER_TASKGROUP_CHANNEL);
            Assert.assertNotNull("have set task channel number", channelNo);
            int taskNumber = taskGroupConfig.getListConfiguration(
                    CoreConstant.DATAX_JOB_CONTENT).size();
            sumSlices += taskNumber;
            Assert.assertTrue("task has average tasks", taskNumber
                    / channelNo == averSlicesPerChannel);
        }

        Assert.assertEquals("slices equal to split sum", tasksNumber, sumSlices);
    }

    @Test
    public void testErrorLimitIgnoreCheck() throws Exception {
        this.configuration.set(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT, -1);
        JobContainer jobContainer = new JobContainer(
                this.configuration);

        Communication communication = new Communication();
        communication.setLongCounter(CommunicationManager.READ_SUCCEED_RECORDS, 100);
        communication.setLongCounter(CommunicationManager.WRITE_RECEIVED_RECORDS, 100);
//        LocalTaskGroupCommunicationManager.updateTaskGroupCommunication(0, communication);

        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("checkLimit");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer, new Object[] {});
        initMethod.setAccessible(false);
    }

    @Test(expected = Exception.class)
    public void testErrorLimitPercentCheck() throws Exception {
//        this.configuration.set(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT, 0.1);
//        this.configuration.set(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT_RECORD, null);
        this.configuration.remove(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT_RECORD);
        this.configuration.set(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT_PERCENT, 0.1);
        JobContainer jobContainer = new JobContainer(
                this.configuration);

        Communication communication = new Communication();
        communication.setLongCounter(CommunicationManager.READ_SUCCEED_RECORDS, 100);
        communication.setLongCounter(CommunicationManager.WRITE_RECEIVED_RECORDS, 80);
        communication.setLongCounter(CommunicationManager.WRITE_FAILED_RECORDS, 20);
//        LocalTaskGroupCommunicationManager.updateTaskGroupCommunication(0, communication);

        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("checkLimit");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer);
        initMethod.setAccessible(false);
    }

    @Test(expected = Exception.class)
    public void testErrorLimitCountCheck() throws Exception {
        this.configuration.remove(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT_PERCENT);
        this.configuration.set(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT_RECORD, 1);
        JobContainer jobContainer = new JobContainer(
                this.configuration);

        Communication communication = new Communication();
        communication.setLongCounter(CommunicationManager.READ_SUCCEED_RECORDS, 100);
        communication.setLongCounter(CommunicationManager.WRITE_RECEIVED_RECORDS, 98);
        communication.setLongCounter(CommunicationManager.WRITE_FAILED_RECORDS, 2);
//        LocalTaskGroupCommunicationManager.updateTaskGroupCommunication(0, communication);

        Method initMethod = jobContainer.getClass()
                .getDeclaredMethod("checkLimit");
        initMethod.setAccessible(true);
        initMethod.invoke(jobContainer);
        initMethod.setAccessible(false);
    }
}
