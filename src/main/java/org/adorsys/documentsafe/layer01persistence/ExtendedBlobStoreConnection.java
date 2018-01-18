package org.adorsys.documentsafe.layer01persistence;

import org.adorsys.documentsafe.layer01persistence.types.complextypes.BucketPath;
import org.adorsys.encobject.domain.ObjectHandle;
import org.adorsys.encobject.service.BlobStoreConnection;
import org.adorsys.encobject.service.BlobStoreContextFactory;
import org.adorsys.encobject.service.ContainerExistsException;
import org.adorsys.encobject.service.UnknownContainerException;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.domain.Location;

/**
 * Created by peter on 16.01.18.
 */
public class ExtendedBlobStoreConnection extends BlobStoreConnection {
    private final BlobStoreContextFactory factory;

    public ExtendedBlobStoreConnection(BlobStoreContextFactory blobStoreContextFactory) {
        super(blobStoreContextFactory);
        this.factory = blobStoreContextFactory;
    }

    @Override
    public void createContainer(String container) throws ContainerExistsException {
        BlobStoreContext blobStoreContext = this.factory.alocate();

        try {
            BucketPath bp = new BucketPath(container);
            if (bp.getDepth() <= 1) {
                BlobStore blobStore = blobStoreContext.getBlobStore();
                if (blobStore.containerExists(container)) {
                    throw new ContainerExistsException(container);
                }
            }
            if (bp.getDepth() > 1) {
                blobStoreContext.getBlobStore().createContainerInLocation((Location) null, bp.getFirstBucket().getValue());
                // TODO nicht schön
                blobStoreContext.getBlobStore().createDirectory(bp.getFirstBucket().getValue(), bp.getSubBuckets());
            } else {
                blobStoreContext.getBlobStore().createContainerInLocation((Location) null, container);
            }
        } finally {
            this.factory.dispose(blobStoreContext);
        }

    }
    @Override
    public void deleteContainer(String container) throws UnknownContainerException {
        BlobStoreContext blobStoreContext = this.factory.alocate();
        try {
            BucketPath bp = new BucketPath(container);
            BlobStore blobStore = blobStoreContext.getBlobStore();
            blobStoreContext.getBlobStore().deleteContainer(bp.getFirstBucket().getValue());
        } finally {
            this.factory.dispose(blobStoreContext);
        }
    }

    @Override
    public boolean containerExists(String container) {
        BlobStoreContext blobStoreContext = this.factory.alocate();

        boolean bucketExists = false;
        try {
            BlobStore blobStore = blobStoreContext.getBlobStore();
            BucketPath bp = new BucketPath(container);
            bucketExists = blobStore.containerExists(bp.getFirstBucket().getValue());
        } finally {
            this.factory.dispose(blobStoreContext);
        }

        return bucketExists;
    }

    /**
     * Achtung, gehört nicht zum derzeitigen Interface
     * @return
     */
    public boolean blobExists(ObjectHandle location) {
        BlobStoreContext blobStoreContext = this.factory.alocate();
        try {
            BlobStore blobStore = blobStoreContext.getBlobStore();
            return blobStore.blobExists(location.getContainer(), location.getName());
        } finally {
            this.factory.dispose(blobStoreContext);
        }
    }

    public PageSet<? extends StorageMetadata> list(String container, boolean recusive) {
        BlobStoreContext blobStoreContext = this.factory.alocate();
        try {
            BlobStore blobStore = blobStoreContext.getBlobStore();
            ListContainerOptions listContainerOptions = new ListContainerOptions();
            if (recusive) {
                listContainerOptions.recursive();
            }
            return blobStore.list(container, listContainerOptions);
        } finally {
            this.factory.dispose(blobStoreContext);
        }
    }

}