# Bix Password Manager
## What is Bix?
Bix is an Login Credentials Manager.
It encrypts and safely stores account login credentials.

## Important Definitions
1. `username`   : The username for the account
2. `password`   : The password for the account
3. `master_key` : The master key for Bix. The master key is required to access all the account logins. It is also needed for saving a new account login credential.
4. `plaintext`  : The string that is formed by appending the username and password. The plaintext is encrypted with AES-128 bit in CBC mode to get the ciphertext which is stored in the 

## How are the Credentials stored?
1. The `username` and `password` are appended together and separated by a whitespace to create the `plaintext`. 
Hence the `plaintext` will look like this: "`username password`"
2. The *PLAINTEXT* is then padded using `PKCS5`.
3. Next, the *PLAINTEXT* is encrypted using `AES-128 bit` in `CBC` mode to get the *CIPHERTEXT*. 
4. Finally, the CIPHERTEXT is encoded in Base64 and stored in the credentials.csv file, along with the ACCOUNT NAME, Initialization Vector (IV), SALT and SHA256 hash of the SECRET KEY.
