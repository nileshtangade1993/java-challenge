package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.NotEnoughFundsException;
import com.db.awmd.challenge.exception.TransferBetweenSameAccountException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransferValidatorImpl implements TransferValidator {

    /**
     * Validates whether the accounts exist, that a transfer cannot happen between same accounts and
     * that there are enough funds to complete the transfer.
     *
     * @param currAccountFrom The existing source account as found in the repository
     * @param currAccountTo The existing destination account as found in the repository
     * @param transfer The transfer object as requested
     * @throws AccountNotFoundException
     * @throws NotEnoughFundsException
     * @throws TransferBetweenSameAccountException
     */
    public void validate(final Account currAccountFrom, final Account currAccountTo, final Transfer transfer)
            throws AccountNotFoundException, NotEnoughFundsException, TransferBetweenSameAccountException{

        if (currAccountFrom == null){
            throw new AccountNotFoundException("Account " + transfer.getAccountFromId() + " not found.");
        }

        if (currAccountTo == null) {
            throw new AccountNotFoundException("Account " + transfer.getAccountToId() + " not found.");
        }

        if (sameAccount(transfer)){
            throw new TransferBetweenSameAccountException("Transfer to self not permitted.");
        }

        if (!enoughFunds(currAccountFrom, transfer.getAmount())){
            throw new NotEnoughFundsException("Not enough funds on account " + currAccountFrom.getAccountId() + " balance="+currAccountFrom.getBalance());
        }
    }

    private boolean sameAccount(final Transfer transfer) {
        return transfer.getAccountFromId().equals(transfer.getAccountToId());
    }


    private boolean enoughFunds(final Account account, final BigDecimal amount) {
        return account.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) >= 0;
    }

}
