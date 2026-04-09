to run project:
1.$env:Path += ";C:\Program Files\apache\Maven\apache-maven-3.9.14\bin"
2. cd bank-api
3. mvn clean install
4. mvn spring-boot:run


Main app: bank-api/src/main/java/com/omkaar/bank/BankApplication.java (@SpringBootApplication)
Key controllers: AccountController, TransactionController, TransferController, LoanController, AuthController, AdminController, etc.
Entities: AccountEntity, TransactionEntity, UserEntity, LoanRequestEntity
Services: BankServiceImpl, PdfStatementService, EmailService, LimitChecker
Exception handler: GlobalExceptionHandler