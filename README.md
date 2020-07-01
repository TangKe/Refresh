# Refresh
A pull refresh wrapper for all scrollable view which implements nested scroll, support refresh vertically or horizontally

## Usage

in your `build.gradle`

```groovy
implementation 'ke.tang:refresh:1.1.0'
```

in your layout xml file

```xml
<ke.tang.refresh.RefreshLayout 
    android:id="@+id/refresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:refreshAxis="vertical">
  <TextView
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:layout_role="header"
      android:text="refresh header" />
  <androidx.recyclerview.widget.RecyclerView
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      app:layout_refresh_role="content"
      app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager" />
  <TextView
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:layout_role="footer"
      android:text="refresh footer" />
</ke.tang.refresh.RefreshLayout>
```

## Attributes

Attribute for `RefreshLayout`

| Attribute   | Value                    | Description                                                  |
| ----------- | ------------------------ | ------------------------------------------------------------ |
| refreshAxis | vertical<br />horizontal | refresh orientation, only support one direction in same time |

Attribute for `RefreshLayout` children

| Attribute           | Value                           | Description                                                  |
| ------------------- | ------------------------------- | ------------------------------------------------------------ |
| layout_refresh_role | content<br />header<br />footer | specified the role in this layout<br />affect the layout logic |
| layout_gravity      |                                 | gravity of this child                                        |

Attribute for `AnimationRefreshView`

| Attribute        | Value             | Description           |
| ---------------- | ----------------- | --------------------- |
| pullAnimation    | raw type resource | lottie animation json |
| refreshAnimation | raw type resource | lottie animation json |

