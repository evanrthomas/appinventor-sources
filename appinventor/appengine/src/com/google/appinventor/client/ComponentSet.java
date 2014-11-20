package com.google.appinventor.client;

import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.ArrayList;
import java.util.Collection;

public class ComponentSet {
  Collection<MockComponent> components;
  public ComponentSet() {
    components = new ArrayList<MockComponent>();
  }

  public ComponentSet(Collection<MockComponent> comps) {
    this();
    for (MockComponent comp: comps) {
      addComponent(comp);
    }
  }

  public void addComponent(MockComponent component) {
    components.add(component);
  }

  public MockComponent getComponentByName(String name) {
    //Note (evan): do an n**2 loop instead of lookup in HashMap because a component's name can change.
    //If we populate a Map<Name, MockComponent> in the constructor, that map may not be valid once this
    //method is called, because the component's name has changed

    //TODO (evan): give MockComponent an addListener method, so we can listen for changes on the MockComponents
    //and maintain a  Map<Name, MockComponent>
    for (MockComponent comp: flatten(components)) {
      if (comp.getName().equals(name)) {
        return comp;
      }
    }
    return null;
  }

  public Collection<MockComponent> getComponents() {
    return flatten(components);
  }

  public static TreeItem buildTree(ComponentSet components) {
    TreeItem itemNode = new TreeItem();
    for (MockComponent comp : components.getComponents()) {
      itemNode.addItem(comp.buildTree());
    }
    itemNode.setState(false);
    return itemNode;
  }


  public static Collection<MockComponent> flatten(MockComponent parent) {
    //TODO (evan): make this return a lazily evaled iterator intead of flattening immediatley
    Collection<MockComponent> toReturn = new ArrayList<MockComponent>();
    toReturn.add(parent);
    for (MockComponent comp: parent.getChildren()) {
      toReturn.addAll(flatten(comp));
    }
    return toReturn;
  }

  public static Collection<MockComponent> flatten(Collection<MockComponent> comps) {
    //TODO (evan): make this return a lazily evaled iterator intead of flattening immediatley
    Collection<MockComponent> toReturn = new ArrayList<MockComponent>();
    for (MockComponent comp: comps) {
      toReturn.addAll(flatten(comp));
    }
    return toReturn;
  }

}
