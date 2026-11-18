package com.paybank.account.repository;

import com.paybank.account.model.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    /**
     * Movements of the account of the given user, most recent first. The order is fixed in the
     * query (the pageable must be unsorted). The counterparty is the recipient for a charge and
     * the payer for a credit.
     */
    @Query(value = """
            select new com.paybank.account.repository.MovementRow(
                m.id, m.createdAt, m.type, m.amount, m.balanceAfter, p.concept,
                case when m.type = com.paybank.account.model.MovementType.CHARGE
                     then destinationUser.alias else originUser.alias end)
            from Movement m
              join m.account a
              join m.payment p
              join p.originAccount originAccount
              join originAccount.user originUser
              join p.destinationAccount destinationAccount
              join destinationAccount.user destinationUser
            where a.user.id = :userId
            order by m.createdAt desc, m.id desc
            """,
            countQuery = "select count(m) from Movement m where m.account.user.id = :userId")
    Page<MovementRow> findPageByUserId(@Param("userId") Long userId, Pageable pageable);
}
