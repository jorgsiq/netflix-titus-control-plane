package io.netflix.titus.master.mesos;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;

@Configuration(prefix = "titus.mesos")
public interface MesosConfiguration {

    @DefaultValue("invalidRequest")
    String getInvalidRequestMessagePattern();

    @DefaultValue("crashed")
    String getCrashedMessagePattern();

    @DefaultValue("transientSystemError")
    String getTransientSystemErrorMessagePattern();

    @DefaultValue("localSystemError")
    String getLocalSystemErrorMessagePattern();

    @DefaultValue("unknownSystemError")
    String getUnknownSystemErrorMessagePattern();

    @DefaultValue("true")
    boolean isReconcilerEnabled();

    @DefaultValue("false")
    boolean isAllowReconcilerUpdatesForUnknownTasks();

    /**
     * @return whether or not the nested containers should be allowed.
     */
    @DefaultValue("false")
    boolean isNestedContainersEnabled();

    /**
     * @return the min that can be set on the killWaitSeconds field. The default value will be used instead if the value specified
     * is lower than the min.
     */
    @DefaultValue("10")
    int getMinKillWaitSeconds();

    /**
     * @return the max that can be set on the killWaitSeconds field. The default value will be used instead if the value specified
     * is greater than the max.
     */
    @DefaultValue("300")
    int getMaxKillWaitSeconds();

    /**
     * @return maximum amount of seconds to wait before forcefully terminating a container.
     */
    @DefaultValue("10")
    int getDefaultKillWaitSeconds();

    /**
     * @return whether or not the ignoreLaunchGuard flag should be sent to the agent on a v3 task.
     */
    @DefaultValue("false")
    boolean isV3IgnoreLaunchGuardEnabled();

    /**
     * @return whether or not to override the executor URI on an agent.
     */
    @DefaultValue("true")
    boolean isExecutorUriOverrideEnabled();

    /**
     * @return the URI to use for all executor URI overrides.
     */
    @DefaultValue("")
    String getGlobalExecutorUriOverride();

    /**
     * @return the command to use with overridden executor URIs.
     */
    @DefaultValue("./run")
    String getExecutorUriOverrideCommand();
}
