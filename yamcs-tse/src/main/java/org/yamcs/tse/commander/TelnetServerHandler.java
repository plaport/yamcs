package org.yamcs.tse.commander;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TelnetServerHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(TelnetServerHandler.class);

    private static final String DEFAULT_PROMPT = "$ ";
    private static final char[] HEXCHARS = "0123456789ABCDEF".toCharArray();

    private DeviceManager deviceManager;
    private boolean printHex;
    private Device currentDevice;

    public TelnetServerHandler(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Telnet client connected: " + ctx.channel().remoteAddress());
        ctx.writeAndFlush("Welcome! Run '?' for more info.\n" + DEFAULT_PROMPT);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String cmd) throws Exception {
        StringWriter out = new StringWriter();
        try {
            if (currentDevice != null) {
                commandDevice(cmd, out);
            } else if (cmd.equals("?") || cmd.equals("help")) {
                out.write(getHelpString());
            } else if (cmd.equals("list")) {
                listDevices(cmd, out);
            } else if (cmd.startsWith("describe")) {
                describeDevice(cmd, out);
            } else if (cmd.startsWith("connect")) {
                connectDevice(cmd, out);
            } else if (!cmd.isEmpty()) {
                out.write(cmd.trim() + ": command not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            out.write("error: " + e.getMessage());
        }

        String response = out.toString();
        if (!response.isEmpty()) {
            ctx.write(response);
            ctx.write("\n");
        }

        if (currentDevice != null) {
            ctx.writeAndFlush(currentDevice.getId() + DEFAULT_PROMPT);
        } else {
            ctx.writeAndFlush(DEFAULT_PROMPT);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Telnet client disconnected: " + ctx.channel().remoteAddress());
    }

    // Does not really 'connect' (this is handled by the DeviceManager), but changes the prompt
    private void connectDevice(String cmd, StringWriter out) throws IOException {
        String deviceId = cmd.split("\\s+", 2)[1];
        Device device = deviceManager.getDevice(deviceId);
        if (device != null) {
            currentDevice = device;
        } else {
            out.write("unknown device");
        }
    }

    private void commandDevice(String cmd, StringWriter out) throws IOException, InterruptedException {
        if ("\\q".equals(cmd)) {
            currentDevice = null;
        } else if ("\\hex".equals(cmd)) {
            printHex = true;
        } else if ("\\ascii".equals(cmd)) {
            printHex = false;
        } else if ("?".equals(cmd) || "help".equals(cmd)) {
            out.write(getDeviceHelpString());
        } else if (!cmd.isEmpty()) {
            try {
                String result = deviceManager.queueCommand(currentDevice, cmd).get();
                if (result != null) {
                    out.write(printHex ? toHex(result.getBytes()) : result);
                }
            } catch (ExecutionException e) {
                out.write(e.getCause().getMessage());
            }
        }
    }

    private void listDevices(String cmd, StringWriter out) {
        out.write(String.format("%-20s %s\n", "ID", "DESCRIPTION"));
        String table = deviceManager.getDevices().stream()
                .map(d -> String.format("%-20s %s", d.getId(), d.getDescription()))
                .collect(Collectors.joining("\n"));
        out.write(table);
    }

    private void describeDevice(String cmd, StringWriter out) {
        String deviceId = cmd.split("\\s+", 2)[1];
        Device device = deviceManager.getDevice(deviceId);
        if (device != null) {
            StringBuilder buf = new StringBuilder();
            if (device instanceof SerialDevice) {
                SerialDevice sDevice = (SerialDevice) device;
                buf.append("locator: serial:").append(sDevice.getPath()).append("\n");
                buf.append("baudrate: ").append(sDevice.getBaudrate()).append("\n");
                buf.append("data bits: ").append(sDevice.getDataBits()).append("\n");
                if (sDevice.getParity() != null) {
                    buf.append("parity: ").append(sDevice.getParity()).append("\n");
                } else {
                    buf.append("parity: none\n");
                }
            }
            buf.append("response timeout (ms): ").append(device.getResponseTimeout()).append("\n");
            if (device.getResponseTermination() != null) {
                buf.append("response termination: ").append(device.getResponseTermination());
            } else {
                buf.append("response termination: none");
            }
            out.write(buf.toString());
        } else {
            out.write("unknown device");
        }
    }

    private String getHelpString() {
        StringBuilder buf = new StringBuilder();
        buf.append("    list           List available devices to manage.\n");
        buf.append("    describe <ID>  Print device configuration details.\n");
        buf.append("    connect <ID>   Connect and interact with a device.");
        return buf.toString();
    }

    private String getDeviceHelpString() {
        StringBuilder buf = new StringBuilder();
        buf.append("    \\hex       Print the hexadecimal value of next responses.\n");
        buf.append("    \\ascii     Print the ASCII value of next responses.\n");
        buf.append("    \\q         Disconnect device.\n");
        buf.append("\n");
        buf.append("    Any other command is sent to the connected device.");
        return buf.toString();
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            chars[j * 2] = HEXCHARS[v >>> 4];
            chars[j * 2 + 1] = HEXCHARS[v & 0x0F];
        }
        return new String(chars);
    }
}
