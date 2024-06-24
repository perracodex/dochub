# [KDoc](https://github.com/perracodex/KDoc)

A secure document storage manager using the [Ktor](https://ktor.io/) framework.

[KDoc](https://github.com/perracodex/KDoc) serves as a comprehensive example of a document storage manager,
allowing to securely upload, download, and manage documents.

### Characteristics:

* **Document Encryption at Rest**:
  All documents are encrypted using industry-standard algorithms before storage, ensuring data security
  even if the storage medium is compromised.

* **Encrypted and Signed Document URLs:**
  URLs are encrypted and signed to prevent tampering and unauthorized access,
  ensuring secure document delivery from end-to-end.

* **Optimized for Large File Transfers:**
  Designed for efficient handling of large file uploads and downloads using streaming encryption
  and decryption to minimize memory usage and enhance performance.

* **Secure Token-Based Access Control:**
  Access is managed through secure, time-limited tokens, ensuring only authenticated and authorized users can access documents.

* **Organized Storage:**
  Uses a hierarchical directory structure and unique identifiers for efficient file organization,
  enabling quick access and retrieval.

* **Audit and Logging:**
  Maintains comprehensive logs of all file access and operations for monitoring, security audits, and forensic analysis.

* **Configurable Encryption Settings:**
  Allows administrators to select encryption algorithms and manage keys, offering customizable security configurations.

---

### Wiki

* ### [Upload](./.wiki/01.upload.md)
* ### [Download](./.wiki/02.download.md)
* ### [Persistence](./.wiki/03.persistence.md)
* ### [Secure URLs](./.wiki/04.secure-url.md)
* ### [Authentication](./.wiki/05.authentication.md)
* ### [Endpoints](./.wiki/06.endpoints.md)

---
For convenience, it is included a *[Postman Collection](./.postman/kdoc.postman_collection.json)* with all the available REST endpoints.
