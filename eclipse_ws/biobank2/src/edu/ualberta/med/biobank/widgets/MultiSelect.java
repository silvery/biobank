package edu.ualberta.med.biobank.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import edu.ualberta.med.biobank.forms.FormUtils;

public class MultiSelect extends Composite {
	
	static Logger log4j = Logger.getLogger(MultiSelect.class.getName());
	
	private TreeViewer selTree;
	
	private TreeViewer availTree;
	
	private MultiSelectNode selTreeRootNode = new MultiSelectNode(null, 0, "selRoot");
	
	private MultiSelectNode availTreeRootNode = new MultiSelectNode(null, 0, "availRoot");
	
	private int minHeight;
	
	HashMap<String, Integer> availableInv;

	public MultiSelect(Composite parent, int style, String leftLabel, 
			String rightLabel, int minHeight) {
		super(parent, style);
		
		availableInv = new HashMap<String, Integer>();
		
		this.minHeight = minHeight;
		
		setLayout(new GridLayout(2, false));
		setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		selTree = createLabelledTree(this, leftLabel);
		selTree.setInput(selTreeRootNode);
		availTree = createLabelledTree(this, rightLabel);
		availTree.setInput(availTreeRootNode);
		
		dragAndDropSupport(availTree, selTree);
		dragAndDropSupport(selTree, availTree);
	}
	
	private TreeViewer createLabelledTree(Composite parent, String label) {
		Composite selComposite = new Composite(parent, SWT.NONE);
		selComposite.setLayout(new GridLayout(1, true));
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		selComposite.setLayoutData(gd);
		
		Label l = new Label(selComposite, SWT.NONE);
		l.setText(label);
		l.setFont(FormUtils.getHeadingFont());
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.horizontalSpan = 2;
		gd.horizontalAlignment = SWT.CENTER;
		l.setLayoutData(gd);
		
		TreeViewer tv = new TreeViewer(selComposite);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = minHeight;
		gd.widthHint = 180;
		tv.getTree().setLayoutData(gd);

		tv.setLabelProvider(new MultiSelectNodeLabelProvider());		
		tv.setContentProvider(new MultiSelectNodeContentProvider());
		
		return tv;
	}
	
	private void dragAndDropSupport(TreeViewer fromList, TreeViewer toList) {
		new TreeViewerDragListener(fromList);
		new TreeViewerDropListener(toList);
	}

	public void adaptToToolkit(FormToolkit toolkit) {
		adaptAllChildren(this, toolkit);
	}
	
	private void adaptAllChildren(Composite container, FormToolkit toolkit) {
		Control[] children = container.getChildren();
		for (Control aChild : children) {
			toolkit.adapt(aChild, true, true);
			if (aChild instanceof Composite) {
				adaptAllChildren((Composite) aChild, toolkit);
			}
		}
	}
	
	public void addAvailable(HashMap<Integer, String> available) {
		// create an inverse map
		for (int key : available.keySet()) {
			availableInv.put(available.get(key), key);
		}
		
		for (int key : available.keySet()) {
			availTreeRootNode.addChild(new MultiSelectNode(availTreeRootNode, key, available.get(key)));
		}
	}
	
	/**
	 * Return the selected items in the order specified by user.
	 * 
	 */
	public List<Integer> getSelected() {
		List<Integer> result = new ArrayList<Integer>();		
		for (MultiSelectNode node : selTreeRootNode.getChildren()) {
			result.add(availableInv.get(node.getName()));
		}		
		return result;
	}
}
