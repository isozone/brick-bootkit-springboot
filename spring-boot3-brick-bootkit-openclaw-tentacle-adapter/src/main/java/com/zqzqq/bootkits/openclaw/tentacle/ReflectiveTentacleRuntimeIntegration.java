package com.zqzqq.bootkits.openclaw.tentacle;

import com.zqzqq.bootkits.openclaw.control.spi.OpenClawRuntimeIntegration;
import com.zqzqq.bootkits.openclaw.protocol.ClientCapabilityDescriptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReflectiveTentacleRuntimeIntegration implements OpenClawRuntimeIntegration {

    private static final String APP_CLASS_NAME = "cloud.aiai.APP";

    @Override
    public String integrationId() {
        return "tentacle-sdk";
    }

    @Override
    public String displayName() {
        return "Tentacle / OpenClaw SDK";
    }

    @Override
    public boolean available() {
        return resolveSdk() != null;
    }

    @Override
    public List<ClientCapabilityDescriptor> capabilities() {
        Object sdk = resolveSdk();
        List<ClientCapabilityDescriptor> capabilities = new ArrayList<>();
        if (sdk == null) {
            return capabilities;
        }

        if (hasMethod(sdk, "cli")) {
            capabilities.add(capability("openclaw.cli", "CLI Facade", "cli"));
        }
        if (hasMethod(sdk, "connectLocalGateway") || hasMethod(sdk, "newGatewayClient")) {
            capabilities.add(capability("gateway.sessions", "Gateway Sessions", "gateway"));
            capabilities.add(capability("gateway.tasks", "Gateway Tasks", "gateway"));
        }
        if (hasMethod(sdk, "browser")) {
            capabilities.add(capability("browser.cdp", "Browser / CDP", "browser"));
        }
        if (hasMethod(sdk, "host")) {
            capabilities.add(capability("host.tools", "Host Tools", "host"));
            capabilities.add(capability("host.process", "Host Process", "host"));
            capabilities.add(capability("host.runtime", "Host Runtime", "host"));
        }
        if (hasMethod(sdk, "catalog")) {
            capabilities.add(capability("catalog.models", "Model Catalog", "catalog"));
        }
        if (hasMethod(sdk, "registry") || hasStaticMethod(APP_CLASS_NAME, "registry")) {
            capabilities.add(capability("registry.unified", "Unified Registry", "registry"));
        }
        return capabilities;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        Object sdk = resolveSdk();
        details.put("sdkPresent", sdk != null);
        if (sdk == null) {
            details.put("reason", "cloud.aiai.APP not found on classpath");
            return details;
        }
        details.put("sdkClass", sdk.getClass().getName());
        details.put("hasCliFacade", hasMethod(sdk, "cli"));
        details.put("hasGatewayFacade", hasMethod(sdk, "connectLocalGateway") || hasMethod(sdk, "newGatewayClient"));
        details.put("hasBrowserFacade", hasMethod(sdk, "browser"));
        details.put("hasHostFacade", hasMethod(sdk, "host"));
        details.put("hasCatalogFacade", hasMethod(sdk, "catalog"));
        details.put("hasRegistryFacade", hasMethod(sdk, "registry") || hasStaticMethod(APP_CLASS_NAME, "registry"));
        String version = queryOpenClawVersion(sdk);
        if (version != null && !version.isBlank()) {
            details.put("openclawVersion", version);
        }
        return details;
    }

    private ClientCapabilityDescriptor capability(String capabilityId, String displayName, String category) {
        ClientCapabilityDescriptor descriptor = new ClientCapabilityDescriptor();
        descriptor.setCapabilityId(capabilityId);
        descriptor.setDisplayName(displayName);
        descriptor.setCategory(category);
        descriptor.setEnabled(Boolean.TRUE);
        return descriptor;
    }

    private Object resolveSdk() {
        try {
            Class<?> appClass = Class.forName(APP_CLASS_NAME);
            Method sdkMethod = appClass.getMethod("sdk");
            return sdkMethod.invoke(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String queryOpenClawVersion(Object sdk) {
        try {
            Object cli = invoke(sdk, "cli");
            Object response = invoke(cli, "queryOpenclawVersion");
            Object data = invoke(response, "data");
            Object value = invoke(data, "value");
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invoke(Object target, String methodName) throws Exception {
        if (target == null) {
            return null;
        }
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private boolean hasMethod(Object target, String methodName) {
        if (target == null) {
            return false;
        }
        try {
            target.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private boolean hasStaticMethod(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            type.getMethod(methodName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
