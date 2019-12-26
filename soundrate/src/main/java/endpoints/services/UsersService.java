package endpoints.services;

import application.entities.*;
import application.model.UsersAgent;
import application.model.exceptions.ConflictingEmailAddressException;
import application.model.exceptions.UserNotFoundException;
import io.jsonwebtoken.*;
import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;


@Path("/")
@Singleton
@Lock(LockType.READ)
public class UsersService {

    private static final int RECOVER_ACCOUNT_TOKEN_TIME_TO_LIVE = 3 * 60 * 60 * 1000;

    @Resource(mappedName = "mail/soundrateMailSession")
    private Session mailSession;

    @Inject
    private UsersAgent usersAgent;
    @Inject
    private Validator validator;

    private JwtParser jwtParser;
    private Jsonb mapper;

    @PostConstruct
    private void init() {
        this.mapper = JsonbBuilder.create();
        this.jwtParser = Jwts.parser();
        this.jwtParser.setSigningKeyResolver(new SigningKeyResolverAdapter() {
            @Override
            public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) {
                final String username = claims.getSubject();
                final User user = UsersService.this.usersAgent.getUser(username);
                return user == null ? null : user.getPassword().getBytes();
            }
        });
    }

    @Path("/get-user")
    @GET
    public Response getUser(@FormParam("username") @NotBlank final String username) {
        final User user = this.usersAgent.getUser(username);
        return Response.ok(this.mapper.toJson(user), MediaType.APPLICATION_JSON).build();
    }

    @Path("/get-user-reviews")
    @GET
    public Response getUserReviews(@FormParam("user") @NotBlank final String username,
                                   @QueryParam("index") @Min(0) final Integer index,
                                   @QueryParam("limit") @Min(1) final Integer limit,
                                   @Context final HttpServletRequest request) {
        final User user = this.usersAgent.getUser(username);
        if (user == null) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        final List<Review> userReviews = this.usersAgent.getUserReviews(user, index, limit);
        return Response.ok(this.mapper.toJson(userReviews), MediaType.APPLICATION_JSON).build();
    }

    @Path("/get-user-votes")
    @GET
    public Response getUserVotes(@FormParam("user") @NotBlank final String username,
                                 @QueryParam("index") @Min(0) final Integer index,
                                 @QueryParam("limit") @Min(1) final Integer limit,
                                 @Context final HttpServletRequest request) {
        final User user = this.usersAgent.getUser(username);
        if (user == null) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        final List<Vote> userVotes = this.usersAgent.getUserVotes(user, index, limit);
        return Response.ok(this.mapper.toJson(userVotes), MediaType.APPLICATION_JSON).build();
    }

    @Path("/get-user-upvotes")
    @GET
    public Response getUserUpvotes(@FormParam("user") @NotBlank final String username,
                                   @QueryParam("index") @Min(0) final Integer index,
                                   @QueryParam("limit") @Min(1) final Integer limit,
                                   @Context final HttpServletRequest request) {
        final User user = this.usersAgent.getUser(username);
        if (user == null) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        final List<Vote> userUpvotes = this.usersAgent.getUserUpvotes(user, index, limit);
        return Response.ok(this.mapper.toJson(userUpvotes), MediaType.APPLICATION_JSON).build();
    }

    @Path("/get-user-downvotes")
    @GET
    public Response getUserDownvotes(@FormParam("user") @NotBlank final String username,
                                     @QueryParam("index") @Min(0) final Integer index,
                                     @QueryParam("limit") @Min(1) final Integer limit,
                                     @Context final HttpServletRequest request) {
        final User user = this.usersAgent.getUser(username);
        if (user == null) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        final List<Vote> userDownvotes = this.usersAgent.getUserDownvotes(user, index, limit);
        return Response.ok(this.mapper.toJson(userDownvotes), MediaType.APPLICATION_JSON).build();
    }

    @Path("/get-user-reports")
    @GET
    public Response getUserReports(@FormParam("user") @NotBlank final String username,
                                   @QueryParam("index") @Min(0) final Integer index,
                                   @QueryParam("limit") @Min(1) final Integer limit,
                                   @Context final HttpServletRequest request) {
        final User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null
                || !(sessionUser.getRole() == User.Role.MODERATOR || sessionUser.getRole() == User.Role.ADMINISTRATOR))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        final User user = this.usersAgent.getUser(username);
        if (user == null) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        final List<Report> userReports = this.usersAgent.getUserReports(user, index, limit);
        return Response.ok(this.mapper.toJson(userReports), MediaType.APPLICATION_JSON).build();
    }

    @Path("/get-user-backlog")
    @GET
    public Response getUserBacklog(@FormParam("user") @NotBlank final String username,
                                   @QueryParam("index") @Min(0) final Integer index,
                                   @QueryParam("limit") @Min(1) final Integer limit,
                                   @Context final HttpServletRequest request) {
        final User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null
                || !(sessionUser.getRole() == User.Role.MODERATOR || sessionUser.getRole() == User.Role.ADMINISTRATOR))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        final User user = this.usersAgent.getUser(username);
        if (user == null) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        final List<BacklogEntry> userBacklog = this.usersAgent.getUserBacklog(user, index, limit);
        return Response.ok(this.mapper.toJson(userBacklog), MediaType.APPLICATION_JSON).build();
    }

    @Path("/update-user-email")
    @POST
    public Response updateUserEmail(@FormParam("username") @NotBlank final String username,
                                    @FormParam("cpassword") @NotBlank final String currentPassword,
                                    @FormParam("nemail") @NotBlank @Email final String newEmail,
                                    @Context final HttpServletRequest request) {
        final User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null || !sessionUser.getUsername().equals(username))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        try {
            final User user = this.usersAgent.getUser(username);
            if (user == null)
                throw new UserNotFoundException();
            if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
                final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                        .getString("error.invalidCredentials");
                return Response.status(Response.Status.UNAUTHORIZED).entity(response).build();
            }
            user.setEmail(newEmail);
            final Set<ConstraintViolation<User>> constraintViolations = this.validator.validate(user);
            if (!constraintViolations.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST).build();
            this.usersAgent.updateUser(user);
        } catch (UserNotFoundException e) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        } catch (ConflictingEmailAddressException e) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.conflictingEmailAddress");
            return Response.status(Response.Status.CONFLICT).entity(response).build();
        }
        return Response.ok().build();
    }

    @Path("/update-user-password")
    @POST
    public Response updateUserPassword(@FormParam("username") @NotBlank final String username,
                                       @FormParam("cpassword") @NotBlank final String currentPassword,
                                       @FormParam("npassword") @NotNull @Pattern(regexp = User.PASSWORD_PATTERN)
                                           final String newPassword,
                                       @Context final HttpServletRequest request) {
        final User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null || !sessionUser.getUsername().equals(username))
            return Response.status(Response.Status.UNAUTHORIZED).build();
        try {
            final User user = this.usersAgent.getUser(username);
            if (user == null)
                throw new UserNotFoundException();
            if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
                final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                        .getString("error.invalidCredentials");
                return Response.status(Response.Status.UNAUTHORIZED).entity(response).build();
            }
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            final Set<ConstraintViolation<User>> constraintViolations = this.validator.validate(user);
            if (!constraintViolations.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST).build();
            this.usersAgent.updateUser(user);
        } catch (UserNotFoundException e) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        return Response.ok().build();
    }

    @Path("/update-user-role")
    @POST
    public Response updateUserRole(@FormParam("username") @NotBlank final String username,
                                   @FormParam("role") @NotNull final User.Role role,
                                   @Context final HttpServletRequest request) {
        final User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser == null || sessionUser.getRole() != User.Role.ADMINISTRATOR)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        try {
            final User user = this.usersAgent.getUser(username);
            if (user == null)
                throw new UserNotFoundException();
            user.setRole(role);
            final Set<ConstraintViolation<User>> constraintViolations = this.validator.validate(user);
            if (!constraintViolations.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST).build();
            this.usersAgent.updateUser(user);
        } catch (UserNotFoundException e) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        return Response.ok().build();
    }

    @Path("/recover-user-account")
    @POST
    public Response recoverUserAccount(@FormParam("email") @NotBlank @Email final String email,
                                       @Context final HttpServletRequest request,
                                       @Context final UriInfo uriInfo) {
        final User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser != null)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        final User user = this.usersAgent.getUserByEmail(email);
        if (user == null) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.emailNotLinked");
            return Response.status(Response.Status.NOT_FOUND).entity(response).build();
        }
        final String token = Jwts.builder()
                .setSubject(user.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + UsersService.RECOVER_ACCOUNT_TOKEN_TIME_TO_LIVE))
                .signWith(SignatureAlgorithm.HS256, user.getPassword().getBytes(StandardCharsets.UTF_8))
                .compact();
        final String passwordRecoveryUrl = uriInfo.getBaseUri() + "reset?token=" + token;
        final ResourceBundle emailTemplateBundle = ResourceBundle.getBundle("i18n/templates/email", request.getLocale());
        final MimeMessage message = new MimeMessage(this.mailSession);
        try {
            message.setSubject(emailTemplateBundle.getString("recover.subject"));
            message.setContent(MessageFormat.format(emailTemplateBundle.getString("recover.body"), passwordRecoveryUrl),
                    "text/plain; charset=utf-8");
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(user.getEmail()));
            Transport.send(message);
        } catch (MessagingException e) {
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    @Path("/reset-user-password")
    @POST
    public Response resetUserPassword(@FormParam("token") final String token,
                                      @FormParam("password") @NotNull @Pattern(regexp = User.PASSWORD_PATTERN)
                                      final String password,
                                      @Context final HttpServletRequest request) {
        final User sessionUser = (User) request.getSession().getAttribute("user");
        if (sessionUser != null)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        try {
            final String username = this.jwtParser.parseClaimsJws(token).getBody().getSubject();
            final User user = this.usersAgent.getUser(username);
            if (user == null)
                throw new UserNotFoundException();
            user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
            final Set<ConstraintViolation<User>> constraintViolations = this.validator.validate(user);
            if (!constraintViolations.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST).build();
            this.usersAgent.updateUser(user);
        } catch (JwtException e) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.invalidLink");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        } catch (UserNotFoundException e) {
            final String response = ResourceBundle.getBundle("i18n/strings/strings", request.getLocale())
                    .getString("error.userNotFound");
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
        return Response.ok().build();
    }

}
