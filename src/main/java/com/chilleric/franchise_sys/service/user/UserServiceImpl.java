package com.chilleric.franchise_sys.service.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.chilleric.franchise_sys.constant.DateTime;
import com.chilleric.franchise_sys.constant.LanguageMessageKey;
import com.chilleric.franchise_sys.dto.common.ListWrapperResponse;
import com.chilleric.franchise_sys.dto.user.UserRequest;
import com.chilleric.franchise_sys.dto.user.UserResponse;
import com.chilleric.franchise_sys.exception.BadSqlException;
import com.chilleric.franchise_sys.exception.InvalidRequestException;
import com.chilleric.franchise_sys.exception.ResourceNotFoundException;
import com.chilleric.franchise_sys.inventory.user.UserInventory;
import com.chilleric.franchise_sys.repository.accessability.Accessability;
import com.chilleric.franchise_sys.repository.common_entity.ViewPoint;
import com.chilleric.franchise_sys.repository.user.User;
import com.chilleric.franchise_sys.repository.user.User.TypeAccount;
import com.chilleric.franchise_sys.repository.user.UserRepository;
import com.chilleric.franchise_sys.service.AbstractService;
import com.chilleric.franchise_sys.utils.DateFormat;

@Service
public class UserServiceImpl extends AbstractService<UserRepository> implements UserService {

  @Value("${default.password}")
  protected String defaultPassword;

  @Autowired
  private UserInventory userInventory;

  @Override
  public void createNewUser(UserRequest userRequest, String loginId) {
    validate(userRequest);
    Map<String, String> error = generateError(UserRequest.class);
    userInventory.findUserByUsername(userRequest.getUsername()).ifPresent(thisName -> {
      error.put("username", LanguageMessageKey.USERNAME_EXISTED);
      throw new InvalidRequestException(error, LanguageMessageKey.USERNAME_EXISTED);
    });
    Date currentTime = DateFormat.getCurrentTime();
    User user = objectMapper.convertValue(userRequest, User.class);
    ObjectId newId = new ObjectId();
    user.set_id(newId);
    user.setPassword(bCryptPasswordEncoder()
        .encode(Base64.getEncoder().encodeToString(defaultPassword.getBytes())));
    user.setTokens("");
    user.setType(TypeAccount.INTERNAL);
    user.setCreated(currentTime);
    user.setModified(currentTime);
    accessabilityRepository
        .addNewAccessability(new Accessability(null, new ObjectId(loginId), newId));
    repository.insertAndUpdate(user);
  }

  public Optional<UserResponse> findOneUserById(String userId) {
    User user = userInventory.findUserById(userId)
        .orElseThrow(() -> new ResourceNotFoundException(LanguageMessageKey.NOT_FOUND_USER));
    return Optional.of(new UserResponse(user.get_id().toString(), user.getUnitId().toString(),
        user.getType(), user.getUsername(), user.getPassword(), user.getGender(), user.getDob(),
        user.getAddress(), user.getFirstName(), user.getLastName(), user.getEmail(),
        user.getPhone(), user.getTokens(),
        DateFormat.toDateString(user.getCreated(), DateTime.YYYY_MM_DD),
        DateFormat.toDateString(user.getModified(), DateTime.YYYY_MM_DD), user.isVerified(),
        user.isVerify2FA(), user.getDeleted()));
  }

  @Override
  public void updateUserById(String userId, UserRequest userRequest, List<ViewPoint> viewPoints) {
    User user = userInventory.findUserById(userId)
        .orElseThrow(() -> new ResourceNotFoundException(LanguageMessageKey.NOT_FOUND_USER));
    viewPointToRequest(userRequest, viewPoints, user);
    validate(userRequest);
    Map<String, String> error = generateError(UserRequest.class);
    userInventory.findUserByEmail(userRequest.getEmail()).ifPresent(thisEmail -> {
      if (thisEmail.get_id().compareTo(user.get_id()) != 0) {
        error.put("email", LanguageMessageKey.EMAIL_TAKEN);
        throw new InvalidRequestException(error, LanguageMessageKey.EMAIL_TAKEN);
      }
    });
    userInventory.findUserByPhone(userRequest.getPhone()).ifPresent(thisPhone -> {
      if (thisPhone.get_id().compareTo(user.get_id()) != 0) {
        error.put("phone", LanguageMessageKey.PHONE_TAKEN);
        throw new InvalidRequestException(error, LanguageMessageKey.PHONE_TAKEN);
      }
    });
    userInventory.findUserByUsername(userRequest.getUsername()).ifPresent(thisUsername -> {
      if (thisUsername.get_id().compareTo(user.get_id()) != 0) {
        error.put("username", LanguageMessageKey.USERNAME_EXISTED);
        throw new InvalidRequestException(error, LanguageMessageKey.USERNAME_EXISTED);
      }
    });
    if (user.getUsername().compareTo("super_admin") == 0) {
      throw new InvalidRequestException(new HashMap<>(), LanguageMessageKey.FORBIDDEN);
    }
    Date currentTime = DateFormat.getCurrentTime();
    User newUser = objectMapper.convertValue(userRequest, User.class);
    newUser.setPassword(user.getPassword());
    newUser.setTokens(user.getTokens());
    newUser.setCreated(user.getCreated());
    newUser.setModified(currentTime);
    newUser.setVerified(user.isVerified());
    newUser.setVerify2FA(user.isVerify2FA());
    newUser.set_id(user.get_id());
    newUser.setType(user.getType());

    repository.insertAndUpdate(newUser);
  }

  @Override
  public void changeStatusUser(String userId) {
    User user = userInventory.findUserById(userId)
        .orElseThrow(() -> new ResourceNotFoundException(LanguageMessageKey.NOT_FOUND_USER));
    if (user.getUsername().compareTo("super_admin") == 0) {
      throw new InvalidRequestException(new HashMap<>(), LanguageMessageKey.FORBIDDEN);
    }
    user.setDeleted(user.getDeleted() == 0 ? 1 : 0);
    user.setModified(DateFormat.getCurrentTime());

    repository.insertAndUpdate(user);
  }

  @Override
  public Optional<ListWrapperResponse<UserResponse>> getYourUsers(Map<String, String> allParams,
      String keySort, int page, int pageSize, String sortField, String loginId) {
    List<String> targets = accessabilityRepository.getListTargetId(loginId)
        .orElseThrow(() -> new BadSqlException(LanguageMessageKey.SERVER_ERROR)).stream()
        .map(access -> access.getTargetId().toString()).collect(Collectors.toList());
    if (targets.size() == 0) {
      return Optional
          .of(new ListWrapperResponse<UserResponse>(new ArrayList<>(), page, pageSize, 0));
    }
    if (allParams.containsKey("_id")) {
      String[] idList = allParams.get("_id").split(",");
      ArrayList<String> check = new ArrayList<>(Arrays.asList(idList));
      for (int i = 0; i < check.size(); i++) {
        if (targets.contains(idList[i])) {
          check.add(idList[i]);
        }
      }
      if (check.size() == 0) {
        return Optional
            .of(new ListWrapperResponse<UserResponse>(new ArrayList<>(), page, pageSize, 0));
      }
      allParams.put("_id", generateParamsValue(check));
    } else {
      allParams.put("_id", generateParamsValue(targets));
    }

    List<User> users = repository.getUsers(allParams, "", page, pageSize, sortField).get();
    return Optional.of(new ListWrapperResponse<UserResponse>(
        users.stream()
            .map(user -> new UserResponse(user.get_id().toString(), user.getUnitId().toString(),
                user.getType(), user.getUsername(), user.getPassword(), user.getGender(),
                user.getDob(), user.getAddress(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), user.getTokens(),
                DateFormat.toDateString(user.getCreated(), DateTime.YYYY_MM_DD),
                DateFormat.toDateString(user.getModified(), DateTime.YYYY_MM_DD), user.isVerified(),
                user.isVerify2FA(), user.getDeleted()))
            .collect(Collectors.toList()),
        page, pageSize, repository.getTotalPage(allParams)));
  }

  @Override
  public Optional<ListWrapperResponse<UserResponse>> getAllUsers(Map<String, String> allParams,
      String keySort, int page, int pageSize, String sortField, String loginId) {
    List<User> users = repository.getUsers(allParams, "", page, pageSize, sortField).get();
    return Optional.of(new ListWrapperResponse<UserResponse>(
        users.stream()
            .map(user -> new UserResponse(user.get_id().toString(), user.getUnitId().toString(),
                user.getType(), user.getUsername(), user.getPassword(), user.getGender(),
                user.getDob(), user.getAddress(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), user.getTokens(),
                DateFormat.toDateString(user.getCreated(), DateTime.YYYY_MM_DD),
                DateFormat.toDateString(user.getModified(), DateTime.YYYY_MM_DD), user.isVerified(),
                user.isVerify2FA(), user.getDeleted()))
            .collect(Collectors.toList()),
        page, pageSize, repository.getTotalPage(allParams)));
  }
}
