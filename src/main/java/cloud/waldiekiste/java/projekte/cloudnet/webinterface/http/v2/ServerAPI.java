package cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2;

import cloud.waldiekiste.java.projekte.cloudnet.webinterface.ProjectMain;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.JsonUtil;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.RequestUtil;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.ResponseUtil;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.UserUtil;
import de.dytanic.cloudnet.lib.server.ServerGroup;
import de.dytanic.cloudnet.lib.user.User;
import de.dytanic.cloudnet.lib.utility.document.Document;
import de.dytanic.cloudnet.web.server.handler.MethodWebHandlerAdapter;
import de.dytanic.cloudnet.web.server.util.PathProvider;
import de.dytanic.cloudnet.web.server.util.QueryDecoder;
import de.dytanic.cloudnetcore.CloudNet;
import de.dytanic.cloudnetcore.network.components.MinecraftServer;
import de.dytanic.cloudnetcore.network.components.Wrapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

public class ServerAPI extends MethodWebHandlerAdapter {
    private final ProjectMain projectMain;

    public ServerAPI(CloudNet cloudNet, ProjectMain projectMain) {
        super("/cloudnet/api/v2/servergroup");
        cloudNet.getWebServer().getWebServerProvider().registerHandler(this);
        this.projectMain = projectMain;
    }
    @SuppressWarnings( "deprecation" )
    @Override
    public FullHttpResponse get(ChannelHandlerContext channelHandlerContext, QueryDecoder queryDecoder, PathProvider pathProvider, HttpRequest httpRequest) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(), HttpResponseStatus.OK);
        ResponseUtil.setHeader(fullHttpResponse, "Content-Type", "application/json;charset=utf-8");
        if (!RequestUtil.hasHeader(httpRequest, "-xcloudnet-user", "-xcloudnet-passwort", "-xcloudnet-message")) {
            return ResponseUtil.xCloudFieldsNotFound(fullHttpResponse);
        }
        String username = RequestUtil.getHeaderValue(httpRequest, "-xcloudnet-user");
        String userpassword = new String(Base64.getDecoder().decode(RequestUtil.getHeaderValue(httpRequest, "-xcloudnet-password").getBytes()));
        if (!CloudNet.getInstance().authorizationPassword(username, userpassword)) {
            return UserUtil.failedAuthorization(fullHttpResponse);
        }
        User user = CloudNet.getInstance().getUser(username);
        switch (RequestUtil.getHeaderValue(httpRequest, "-Xmessage").toLowerCase()) {
            case "groups":{
                if(!UserUtil.hasPermission(user,"cloudnet.web.group.servers","*")){
                    return ResponseUtil.permissionDenied(fullHttpResponse);
                }
                List<String> groups = new ArrayList<>(getProjectMain().getCloud().getServerGroups().keySet());
                Document resp = new Document();
                resp.append("response", groups);
                return ResponseUtil.success(fullHttpResponse,true,resp);
            }
            case "groupitems":{
                List<String> proxys = new ArrayList<>();
                List<String> infos = new ArrayList<>(getProjectMain().getCloud().getServerGroups().keySet());
                for (String prx : infos) {
                    if(UserUtil.hasPermission(user,"*","cloudnet.web.group.server.item.*","cloudnet.web.proxy.group.server.item."+prx)){
                        ServerGroup group = getProjectMain().getCloud().getServerGroup(prx);
                        Document document = new Document();
                        document.append("name",group.getName());
                        document.append("type",group.getServerType().name());
                        document.append("status",group.isMaintenance());
                        proxys.add(document.convertToJson());
                    }
                }
                Document resp = new Document();
                resp.append("response", proxys);
                return ResponseUtil.success(fullHttpResponse,true,resp);
            }
            case "screen":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getServers().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    MinecraftServer server = getProjectMain().getCloud().getServer(group);
                    if(!UserUtil.hasPermission(user,"cloudnet.web.screen.servers.info.*","*","cloudnet.web.screen.servers.info."+group,"cloudnet.web.screen.servers.info.group."+server.getServiceId().getGroup())){
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    if (!getProjectMain().getCloud().getScreenProvider().getScreens().containsKey(server.getServiceId().getServerId())) {
                        server.getWrapper().enableScreen(server.getServerInfo());
                    }
                    Document resp = new Document();
                    if(getProjectMain().getScreenInfos().containsKey(server.getServiceId().getServerId())){
                        resp.append("response",getProjectMain().getScreenInfos().get(server.getServiceId().getServerId()));
                    }
                    return ResponseUtil.success(fullHttpResponse,true,resp);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "servers":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getServerGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.servers.info.*","*","cloudnet.web.group.servers.info."+group)){
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    List<String> servers = new ArrayList<>();
                    getProjectMain().getCloud().getServers(group).forEach(t->servers.add(JsonUtil.getGson().toJson(t.getLastServerInfo().toSimple())));
                    Document resp = new Document();
                    resp.append("response",servers);
                    return ResponseUtil.success(fullHttpResponse,true,resp);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "group":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getServerGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.server.info.*","*","cloudnet.web.group.server.info."+group)){
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    Document data = new Document();
                    data.append(group,JsonUtil.getGson().toJson(getProjectMain().getCloud().getServerGroup(group)));
                    Document resp = new Document();
                    resp.append("response",data);
                    return ResponseUtil.success(fullHttpResponse,true,resp);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            default:{
                return ResponseUtil.xMessageFieldNotFound(fullHttpResponse);
            }
        }
    }
    @SuppressWarnings("deprecation")
    @Override
    public FullHttpResponse post(ChannelHandlerContext channelHandlerContext, QueryDecoder queryDecoder, PathProvider pathProvider, HttpRequest httpRequest) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(), HttpResponseStatus.OK);
        ResponseUtil.setHeader(fullHttpResponse,"Content-Type", "application/json");
        if (!RequestUtil.hasHeader(httpRequest,"-xcloudnet-user","-xcloudnet-passwort","-xcloudnet-message")) {
            return ResponseUtil.xCloudFieldsNotFound(fullHttpResponse);
        }
        String username = RequestUtil.getHeaderValue(httpRequest,"-xcloudnet-user");
        String userpassword = new String(Base64.getDecoder().decode(RequestUtil.getHeaderValue(httpRequest, "-xcloudnet-password").getBytes()));
        if (!CloudNet.getInstance().authorizationPassword(username, userpassword)) {
            return UserUtil.failedAuthorization(fullHttpResponse);
        }
        User user = CloudNet.getInstance().getUser(username);
        switch (RequestUtil.getHeaderValue(httpRequest,"-Xmessage").toLowerCase()){
            case "stop":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getProxyGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.server.stop.*","*","cloudnet.web.group.server.stop."+group)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    getProjectMain().getCloud().getServers(group).forEach(t->getProjectMain().getCloud().stopServer(t.getName()));
                    Document document = new Document();
                    return ResponseUtil.success(fullHttpResponse,true,document);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "command":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") && RequestUtil.hasHeader(httpRequest,"-Xcount")){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    final String command = RequestUtil.getHeaderValue(httpRequest,"-Xcount");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.screen.server.command.*","*","cloudnet.web.screen.server.command."+command.split(" ")[0])) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    MinecraftServer server = getProjectMain().getCloud().getServer(group);
                    server.getWrapper().writeServerCommand(command,server.getServerInfo());
                    Document document = new Document();
                    return ResponseUtil.success(fullHttpResponse,true,document);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "stopscreen":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getScreenProvider().getScreens().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.screen.server.stop.*","*","cloudnet.web.screen.server.stop."+group)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    MinecraftServer server = getProjectMain().getCloud().getServer(group);
                    server.getWrapper().disableScreen(server.getServerInfo());
                    Document document = new Document();
                    return ResponseUtil.success(fullHttpResponse,true,document);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "delete":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getProxyGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.server.delete.*","*","cloudnet.web.group.server.delete."+group)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    getProjectMain().getCloud().getServers(group).forEach(t->getProjectMain().getCloud().stopServer(t.getName()));
                    ServerGroup grp = getProjectMain().getCloud().getServerGroup(group);
                    CloudNet.getInstance().getServerGroups().remove(grp.getName());
                    Collection<String> wrps = grp.getWrapper();
                    getProjectMain().getCloud().getConfig().deleteGroup(grp);
                    CloudNet.getInstance().toWrapperInstances(wrps).forEach(Wrapper::updateWrapper);
                    Document document = new Document();
                    return ResponseUtil.success(fullHttpResponse,true,document);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "save":{
                final String servergroup = RequestUtil.getContent(httpRequest);
                ServerGroup serverGroup = JsonUtil.getGson().fromJson(servergroup,ServerGroup.class);
                if(!UserUtil.hasPermission(user,"cloudnet.web.group.server.save.*","*","cloudnet.web.group.server.save."+serverGroup.getName())) {
                    return ResponseUtil.permissionDenied(fullHttpResponse);
                }
                Paths.get("groups/" + serverGroup.getName() + ".json").toFile().deleteOnExit();

                getProjectMain().getCloud().getConfig().createGroup(serverGroup);
                CloudNet.getInstance().setupGroup(serverGroup);
                if(!CloudNet.getInstance().getServerGroups().containsKey(serverGroup.getName())){
                    CloudNet.getInstance().getServerGroups().put(serverGroup.getName(), serverGroup);
                }else{
                    CloudNet.getInstance().getServerGroups().replace(serverGroup.getName(),serverGroup);
                }
                CloudNet.getInstance().toWrapperInstances(serverGroup.getWrapper()).forEach(Wrapper::updateWrapper);
                Document document = new Document();
                return ResponseUtil.success(fullHttpResponse,true,document);
            }
            case "start":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue","-xCount") &&
                        getProjectMain().getCloud().getProxyGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    final int count = Integer.valueOf(RequestUtil.getHeaderValue(httpRequest,"-Xcount"));
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.server.start.*","*","cloudnet.web.group.server.start."+group /*,"cloudnet.web.group.proxy.start."+group+"."+count,"cloudnet.web.group.proxy.start."+count*/)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    for (int i = 0; i < count; i++) {
                        getProjectMain().getCloud().startGameServer(getProjectMain().getCloud().getServerGroup(group));
                    }
                    Document document = new Document();
                    return ResponseUtil.success(fullHttpResponse,true,document);
                }else{
                    return ResponseUtil.xFieldNotFound(fullHttpResponse,"No available -Xvalue,-Xcount command found!");
                }
            }
            default:{
                return ResponseUtil.xMessageFieldNotFound(fullHttpResponse);
            }
        }
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public FullHttpResponse options(ChannelHandlerContext channelHandlerContext, QueryDecoder queryDecoder, PathProvider pathProvider, HttpRequest httpRequest) {
        return ResponseUtil.cross(httpRequest);
    }

    private ProjectMain getProjectMain() {
        return projectMain;
    }
}