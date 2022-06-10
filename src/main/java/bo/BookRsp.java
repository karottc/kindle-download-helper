package bo;

import java.util.List;

/**
 * @author karottc@gmail.com
 * @date 2022-06-10 23:55
 */
public class BookRsp {
    public Data OwnershipData;

    public Data getOwnershipData() {
        return OwnershipData;
    }

    public void setOwnershipData(Data ownershipData) {
        OwnershipData = ownershipData;
    }

    public static class Data {
        public boolean hasMoreItems;
        public int numberOfItems;
        public boolean success;
        public List<BookItem> items;

        public boolean isHasMoreItems() {
            return hasMoreItems;
        }

        public void setHasMoreItems(boolean hasMoreItems) {
            this.hasMoreItems = hasMoreItems;
        }

        public int getNumberOfItems() {
            return numberOfItems;
        }

        public void setNumberOfItems(int numberOfItems) {
            this.numberOfItems = numberOfItems;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public List<BookItem> getItems() {
            return items;
        }

        public void setItems(List<BookItem> items) {
            this.items = items;
        }
    }
}
