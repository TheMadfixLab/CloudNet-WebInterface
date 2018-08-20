package cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2;

import cloud.waldiekiste.java.projekte.cloudnet.webinterface.ProjectMain;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.JsonUtil;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.RequestUtil;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.ResponseUtil;
import cloud.waldiekiste.java.projekte.cloudnet.webinterface.http.v2.utils.UserUtil;
import de.dytanic.cloudnet.lib.server.ProxyGroup;
import de.dytanic.cloudnet.lib.user.User;
import de.dytanic.cloudnet.lib.utility.document.Document;
import de.dytanic.cloudnet.web.server.handler.MethodWebHandlerAdapter;
import de.dytanic.cloudnet.web.server.util.PathProvider;
import de.dytanic.cloudnet.web.server.util.QueryDecoder;
import de.dytanic.cloudnetcore.CloudNet;
import de.dytanic.cloudnetcore.network.components.ProxyServer;
import de.dytanic.cloudnetcore.network.components.Wrapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

public class ProxyAPI extends MethodWebHandlerAdapter {
    private final ProjectMain projectMain;

    public ProxyAPI(CloudNet cloudNet, ProjectMain projectMain) {
        super("/cloudnet/api/v2/proxygroup");
        cloudNet.getWebServer().getWebServerProvider().registerHandler(this);
        this.projectMain = projectMain;
    }
    @SuppressWarnings( "deprecation" )
    @Override
    public FullHttpResponse get(ChannelHandlerContext channelHandlerContext, QueryDecoder queryDecoder, PathProvider pathProvider, HttpRequest httpRequest){
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(), HttpResponseStatus.OK);
        ResponseUtil.setHeader(fullHttpResponse,"Content-Type", "application/json; charset=utf-8");
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
            case "groups":{
                if(!UserUtil.hasPermission(user,"cloudnet.web.group.proxys","*")){
                    return ResponseUtil.permissionDenied(fullHttpResponse);
                }
                List<String> groups = new ArrayList<>(getProjectMain().getCloud().getProxyGroups().keySet());
                Document resp = new Document();
                resp.append("response", groups);
                return ResponseUtil.success(fullHttpResponse,true,resp);
            }
            case "groupitems":{
                List<String> proxys = new ArrayList<>();
                List<String> infos = new ArrayList<>();
                getProjectMain().getCloud().getProxyGroups().keySet().forEach(infos::add);
                for (String prx : infos) {
                    if(!UserUtil.hasPermission(user,"*","cloudnet.web.group.proxy.item.*","cloudnet.web.proxy.group.proxy.item."+prx)){
                        continue;
                    }else{
                        ProxyGroup group = getProjectMain().getCloud().getProxyGroups().get(prx);
                        Document document = new Document();
                        document.append("name",group.getName());
                        document.append("version",group.getProxyVersion().name());
                        document.append("status",group.getProxyConfig().isEnabled());
                        proxys.add(document.convertToJson());
                    }
                }
                Document resp = new Document();
                resp.append("response", proxys);
                return ResponseUtil.success(fullHttpResponse,true,resp);
            }
            case "group":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getProxyGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.proxy.info.*","*","cloudnet.web.group.proxy.info."+group)){
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    Document data = new Document();
                    data.append(group,JsonUtil.getGson().toJson(getProjectMain().getCloud().getProxyGroup(group)));
                    Document resp = new Document();
                    resp.append("response",data);
                    return ResponseUtil.success(fullHttpResponse,true,resp);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "screen":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getProxys().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    ProxyServer server = getProjectMain().getCloud().getProxy(group);
                    if(!UserUtil.hasPermission(user,"cloudnet.web.screen.proxys.info.*","*","cloudnet.web.screen.proxys.info."+group,"cloudnet.web.screen.proxys.info.group."+server.getServiceId().getGroup())){
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    if (!getProjectMain().getCloud().getScreenProvider().getScreens().containsKey(server.getServiceId().getServerId())) {
                        server.getWrapper().enableScreen(server.getProxyInfo());
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
            case "proxys":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getProxyGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.proxys.info.*","*","cloudnet.web.group.proxys.info."+group)){
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    List<String> servers = new ArrayList<>();
                    getProjectMain().getCloud().getProxys(group).forEach(t->servers.add(JsonUtil.getGson().toJson(t.getProxyInfo().toSimple())));
                    Document resp = new Document();
                    resp.append("response",servers);
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
    @SuppressWarnings( "deprecation" )
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
            case "command":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") && RequestUtil.hasHeader(httpRequest,"-Xcount")){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    final String command = RequestUtil.getHeaderValue(httpRequest,"-Xcount");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.screen.proxy.command.*","*","cloudnet.web.screen.proxy.command."+command.split(" ")[0])) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    ProxyServer server = getProjectMain().getCloud().getProxy(group);
                    server.getWrapper().writeProxyCommand(command,server.getProxyInfo());
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
                    if(!UserUtil.hasPermission(user,"cloudnet.web.screen.proxy.stop.*","*","cloudnet.web.screen.proxy.stop."+group)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    ProxyServer server = getProjectMain().getCloud().getProxy(group);
                    server.getWrapper().disableScreen(server.getProxyInfo());
                    Document document = new Document();
                    return ResponseUtil.success(fullHttpResponse,true,document);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "stopproxy":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue")){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.proxy.stop.*","*","cloudnet.web.proxy.stop."+group)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    getProjectMain().getCloud().stopProxy(group);
                    Document document = new Document();
                    return ResponseUtil.success(fullHttpResponse,true,document);
                }else{
                    return ResponseUtil.xValueFieldNotFound(fullHttpResponse);
                }
            }
            case "stop":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue") &&
                        getProjectMain().getCloud().getProxyGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.proxy.stop.*","*","cloudnet.web.group.proxy.stop."+group)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    getProjectMain().getCloud().getProxys(group).forEach(t->getProjectMain().getCloud().stopProxy(t.getName()));
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
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.proxy.delete.*","*","cloudnet.web.group.proxy.delete."+group)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    ProxyGroup grp = getProjectMain().getCloud().getProxyGroup(group);
                    CloudNet.getInstance().getProxyGroups().remove(grp.getName());
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
                final String proxygroup = RequestUtil.getContent(httpRequest);
                ProxyGroup proxygn = JsonUtil.getGson().fromJson(proxygroup,ProxyGroup.class);
                if(!UserUtil.hasPermission(user,"cloudnet.web.group.proxy.save.*","*","cloudnet.web.group.proxy.save."+proxygn.getName())) {
                    return ResponseUtil.permissionDenied(fullHttpResponse);
                }
                getProjectMain().getCloud().getConfig().createGroup(proxygn);
                CloudNet.getInstance().setupProxy(proxygn);
                if(!CloudNet.getInstance().getProxyGroups().containsKey(proxygn.getName())){
                    CloudNet.getInstance().getProxyGroups().put(proxygn.getName(), proxygn);
                }else{
                    CloudNet.getInstance().getProxyGroups().replace(proxygn.getName(),proxygn);
                }
                CloudNet.getInstance().toWrapperInstances(proxygn.getWrapper()).forEach(Wrapper::updateWrapper);
                Document document = new Document();
                return ResponseUtil.success(fullHttpResponse,true,document);
            }
            case "start":{
                if(RequestUtil.hasHeader(httpRequest,"-Xvalue","-xCount") &&
                        getProjectMain().getCloud().getProxyGroups().containsKey(RequestUtil.getHeaderValue(httpRequest,"-Xvalue"))){
                    final String group = RequestUtil.getHeaderValue(httpRequest,"-Xvalue");
                    final int count = Integer.valueOf(RequestUtil.getHeaderValue(httpRequest,"-Xcount"));
                    if(!UserUtil.hasPermission(user,"cloudnet.web.group.proxy.start.*","*","cloudnet.web.group.proxy.start."+group /*,"cloudnet.web.group.proxy.start."+group+"."+count,"cloudnet.web.group.proxy.start."+count*/)) {
                        return ResponseUtil.permissionDenied(fullHttpResponse);
                    }
                    for (int i = 0; i < count; i++) {
                        getProjectMain().getCloud().startProxyAsync(getProjectMain().getCloud().getProxyGroup(group));
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
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpRequest.getProtocolVersion(), HttpResponseStatus.OK);
        fullHttpResponse.headers().set("Content-Type", "application/json");
        fullHttpResponse.headers().set("Access-Control-Allow-Credentials", "true");
        fullHttpResponse.headers().set("Access-Control-Allow-Headers", "content-type, if-none-match, -Xcloudnet-token, -Xmessage, -Xvalue, -Xcloudnet-user, -Xcloudnet-password,-Xcount");
        fullHttpResponse.headers().set("Access-Control-Allow-Methods", "POST,GET,OPTIONS");
        fullHttpResponse.headers().set("Access-Control-Allow-Origin", "*");
        fullHttpResponse.headers().set("Access-Control-Max-Age", "3600");
        return fullHttpResponse;
    }

    private ProjectMain getProjectMain() {
        return projectMain;
    }
}
