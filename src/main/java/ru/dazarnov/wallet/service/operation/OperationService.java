package ru.dazarnov.wallet.service.operation;

import ru.dazarnov.wallet.dto.OperationTO;
import ru.dazarnov.wallet.exception.OperationServiceException;
import ru.dazarnov.wallet.exception.UnknownAccountException;

import java.util.Optional;

public interface OperationService {

    void create(OperationTO operationTO) throws UnknownAccountException, OperationServiceException;

    Optional<OperationTO> findById(long id) throws OperationServiceException;

}
