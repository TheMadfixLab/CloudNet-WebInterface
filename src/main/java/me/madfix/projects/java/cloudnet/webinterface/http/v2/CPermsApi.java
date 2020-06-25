package me.madfix.projects.java.cloudnet.webinterface.http.v2;

import me.madfix.projects.java.cloudnet.webinterface.ProjectMain;
import me.madfix.projects.java.cloudnet.webinterface.http.v2.utils.Http;
import me.madfix.projects.java.cloudnet.webinterface.http.v2.utils.HttpUser;
import me.madfix.projects.java.cloudnet.webinterface.http.v2.utils.JsonUtil;
import me.madfix.projects.java.cloudnet.webinterface.http.v2.utils.Request;
import me.madfix.projects.java.cloudnet.webinterface.http.v2.utils.Response;
import de.dytanic.cloudnet.lib.NetworkUtils;
import de.dytanic.cloudnet.lib.player.CloudPlayer;
import de.dytanic.cloudnet.lib.player.OfflinePlayer;
import de.dytanic.cloudnet.lib.player.permission.PermissionGroup;
import de.dytanic.cloudnet.lib.player.permission.PermissionPool;
import de.dytanic.cloudnet.lib.user.User;
import de.dytanic.cloudnet.lib.utility.document.Document;
import de.dytanic.cloudnet.web.server.handler.MethodWebHandlerAdapter;
import de.dytanic.cloudnet.web.server.util.PathProvider;
import de.dytanic.cloudnet.web.server.util.QueryDecoder;
import de.dytanic.cloudnetcore.CloudNet;
import de.dytanic.cloudnetcore.network.packet.out.PacketOutUpdateOfflinePlayer;
import de.dytanic.cloudnetcore.network.packet.out.PacketOutUpdatePlayer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class CPermsApi extends MethodWebHandlerAdapter {

  private final ProjectMain projectMain;
  private PermissionPool pool;

  /**
   * Initiated the class.
   * @param projectMain The main class of the project
   */
  public CPermsApi(ProjectMain projectMain) {
    super("/cloudnet/api/v2/cperms");
    this.projectMain = projectMain;
    projectMain.getCloud().getWebServer().getWebServerProvider().registerHandler(this);
    pool = projectMain.getCloud().getNetworkManager().getModuleProperties()
        .getObject("permissionPool",
            PermissionPool.TYPE);
  }

  @SuppressWarnings("deprecation")
  @Override
  public FullHttpResponse get(ChannelHandlerContext channelHandlerContext,
      QueryDecoder queryDecoder,
      PathProvider pathProvider, HttpRequest httpRequest) {
    pool = projectMain.getCloud().getNetworkManager().getModuleProperties()
        .getObject("permissionPool",
            PermissionPool.TYPE);
    FullHttpResponse fullHttpResponse = Http.simpleCheck(httpRequest);
    User user = CloudNet.getInstance()
        .getUser(Request.headerValue(httpRequest, "-xcloudnet-user"));
    Document document = new Document();
    switch (Request.headerValue(httpRequest, "-Xmessage").toLowerCase(Locale.ENGLISH)) {
      case "group":
        if (Request.hasHeader(httpRequest, "-Xvalue")) {
          String group = Request.headerValue(httpRequest, "-Xvalue");
          if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.info.group.*", "*",
              "cloudnet.web.cperms.info.group." + group)) {
            return Response.permissionDenied(fullHttpResponse);
          } else {
            if (!pool.isAvailable()) {
              return Response.badRequest(fullHttpResponse,  document);
            }
            document.append("response", JsonUtil.getGson().toJson(pool.getGroups().get(group)));
            return Response.success(fullHttpResponse,  document);
          }
        } else {
          if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.groups", "*")) {
            return Response.permissionDenied(fullHttpResponse);
          } else {
            if (!pool.isAvailable()) {
              return Response.badRequest(fullHttpResponse,  document);
            }
            document.append("response", pool.getGroups().values().stream()
                .map(permissionGroup -> JsonUtil.getGson().toJson(permissionGroup)).collect(
                    Collectors.toList()));
            return Response.success(fullHttpResponse,  document);
          }
        }

      case "groups":
        if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.info.groups.*", "*")) {
          return Response.permissionDenied(fullHttpResponse);
        } else {
          if (!pool.isAvailable()) {
            return Response.badRequest(fullHttpResponse,  document);
          }
          document.append("response", new ArrayList<>(pool.getGroups().keySet()));
          return Response.success(fullHttpResponse,  document);
        }

      case "user":
        if (Request.hasHeader(httpRequest, "-Xvalue")) {
          String userUuid = Request.headerValue(httpRequest, "-Xvalue");
          if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.info.user.*", "*",
              "cloudnet.web.cperms.info.user." + userUuid)) {
            return Response.permissionDenied(fullHttpResponse);
          } else {
            if (!pool.isAvailable()) {
              return Response.badRequest(fullHttpResponse,  document);
            }
            if (!CloudNet.getInstance().getDbHandlers().getNameToUUIDDatabase().getDatabase()
                .contains(userUuid)) {
              return Response.success(fullHttpResponse,  document);
            }
            if (userUuid.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
              document.append("response", JsonUtil.getGson().toJson(this.projectMain.getCloud()
                  .getDbHandlers().getPlayerDatabase().getPlayer(UUID.fromString(userUuid))));
            } else {
              UUID id = CloudNet.getInstance().getDbHandlers().getNameToUUIDDatabase()
                  .get(userUuid);
              document.append("response", JsonUtil.getGson().toJson(this.projectMain.getCloud()
                  .getDbHandlers().getPlayerDatabase().getPlayer(id)));
            }
            return Response.success(fullHttpResponse,  document);
          }
        } else {
          return Response.valueFieldNotFound(fullHttpResponse);
        }
      case "check":
        if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.check", "*")) {
          return Response.permissionDenied(fullHttpResponse);
        }
        if (pool.isAvailable()) {
          return Response.success(fullHttpResponse,  document);
        } else {
          return Response.badRequest(fullHttpResponse,  document);
        }

      default:
        return Response.messageFieldNotFound(fullHttpResponse);

    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public FullHttpResponse post(ChannelHandlerContext channelHandlerContext,
      QueryDecoder queryDecoder,
      PathProvider pathProvider, HttpRequest httpRequest) {
    FullHttpResponse fullHttpResponse = Http.simpleCheck(httpRequest);
    Response.setHeader(fullHttpResponse, "Content-Type", "application/json");
    User user = CloudNet.getInstance()
        .getUser(Request.headerValue(httpRequest, "-xcloudnet-user"));
    Document document = new Document();
    switch (Request.headerValue(httpRequest, "-Xmessage").toLowerCase(Locale.ENGLISH)) {
      case "group":
        String servergroup = Request.content(httpRequest);
        if (servergroup.isEmpty()) {
          return Response.badRequest(fullHttpResponse,  new Document());
        }
        PermissionGroup permissionGroup = JsonUtil.getGson()
            .fromJson(servergroup, PermissionGroup.class);
        if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.group.save.*", "*",
            "cloudnet.web.cperms.group.save." + permissionGroup.getName())) {
          return Response.permissionDenied(fullHttpResponse);
        }
        this.projectMain.getConfigPermission().updatePermissionGroup(permissionGroup);
        NetworkUtils.addAll(pool.getGroups(), this.projectMain.getConfigPermission().loadAll());

        CloudNet.getInstance().getNetworkManager().getModuleProperties().append("permissionPool",
            this.pool);
        CloudNet.getInstance().getNetworkManager().updateAll();
        return Response.success(fullHttpResponse,  document);

      case "deletegroup":
        if (Request.hasHeader(httpRequest, "-Xvalue")) {
          final String group = Request.headerValue(httpRequest, "-Xvalue");
          if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.group.delete.*", "*",
              "cloudnet.web.cperms.group.delete." + group)) {
            return Response.permissionDenied(fullHttpResponse);
          }
          this.pool.getGroups().remove(group);
          CloudNet.getInstance().getNetworkManager().getModuleProperties().append("permissionPool",
              this.pool);
          CloudNet.getInstance().getNetworkManager().updateAll();
          return Response.success(fullHttpResponse,  document);

        } else {
          return Response.valueFieldNotFound(fullHttpResponse);
        }
      case "user":
        final String userString = Request.content(httpRequest);
        if (userString.isEmpty()) {
          return Response.badRequest(fullHttpResponse,  new Document());
        }
        OfflinePlayer offlinePlayer = JsonUtil.getGson().fromJson(userString, OfflinePlayer.class);
        if (!HttpUser.hasPermission(user, "cloudnet.web.cperms.user.save.*", "*",
            "cloudnet.web.cperms.user.save." + offlinePlayer.getName(),
            "cloudnet.web.cperms.user.save." + offlinePlayer.getUniqueId().toString())) {
          return Response.permissionDenied(fullHttpResponse);
        }
        CloudNet.getInstance().getDbHandlers().getPlayerDatabase()
            .updatePermissionEntity(offlinePlayer
                .getUniqueId(), offlinePlayer.getPermissionEntity());

        CloudNet.getInstance().getNetworkManager()
            .sendAllUpdate(new PacketOutUpdateOfflinePlayer(CloudNet
                .getInstance().getDbHandlers().getPlayerDatabase()
                .getPlayer(offlinePlayer.getUniqueId())));

        CloudPlayer onlinePlayer = CloudNet.getInstance().getNetworkManager()
            .getOnlinePlayer(offlinePlayer.getUniqueId());
        if (onlinePlayer != null) {
          onlinePlayer.setPermissionEntity(offlinePlayer.getPermissionEntity());
          CloudNet.getInstance().getNetworkManager()
              . sendAllUpdate(new PacketOutUpdatePlayer(onlinePlayer));
        }
        CloudNet.getInstance().getNetworkManager().updateAll();
        return Response.success(fullHttpResponse,  document);

      default:
        return Response.messageFieldNotFound(fullHttpResponse);

    }

  }

  @Override
  public FullHttpResponse options(ChannelHandlerContext channelHandlerContext,
      QueryDecoder queryDecoder,
      PathProvider pathProvider, HttpRequest httpRequest) {
    return Response.cross(httpRequest);
  }
}